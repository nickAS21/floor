package org.nickas21.smart.data.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.facilities.TelegramHttpClientBuilder;
import org.telegram.telegrambots.meta.api.methods.updates.GetUpdates;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.BotOptions;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.meta.generics.LongPollingBot;
import org.telegram.telegrambots.meta.generics.UpdatesHandler;
import org.telegram.telegrambots.meta.generics.UpdatesReader;
import org.telegram.telegrambots.meta.generics.BackOff;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.telegram.telegrambots.Constants.SOCKET_TIMEOUT;

@Slf4j
public class TelegramBotSession implements BotSession {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger startConflict409 = new AtomicInteger(0);

    private final ConcurrentLinkedDeque<Update> receivedUpdates = new ConcurrentLinkedDeque<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private ReaderThread readerThread;
    private HandlerThread handlerThread;
    private LongPollingBot callback;
    private String token;
    private int lastReceivedUpdate = 0;
    private DefaultBotOptions options;
    private UpdatesSupplier updatesSupplier;

    public TelegramBotSession() {}

    @Override
    public synchronized void start() {
        if (running.get()) {
            throw new IllegalStateException("Session already running");
        }

        running.set(true);
        lastReceivedUpdate = 0;

        readerThread = new ReaderThread(updatesSupplier, this);
        readerThread.setName(callback.getBotUsername() + " TFloorCon");
        readerThread.start();

        handlerThread = new HandlerThread();
        handlerThread.setName(callback.getBotUsername() + " TFloorEx");
        handlerThread.start();
    }

    @Override
    public synchronized void stop() {
        if (!running.get()) {
            throw new IllegalStateException("Session already stopped");
        }

        running.set(false);

        if (readerThread != null) {
            readerThread.interrupt();
        }

        if (handlerThread != null) {
            handlerThread.interrupt();
        }

        if (callback != null) {
            callback.onClosing();
        }
    }

    public void setUpdatesSupplier(UpdatesSupplier updatesSupplier) {
        this.updatesSupplier = updatesSupplier;
    }

    @Override
    public void setOptions(BotOptions options) {
        if (this.options != null) {
            throw new InvalidParameterException("BotOptions has already been set");
        }
        this.options = (DefaultBotOptions) options;
    }

    @Override
    public void setToken(String token) {
        if (this.token != null) {
            throw new InvalidParameterException("Token has already been set");
        }
        this.token = token;
    }

    @Override
    public void setCallback(LongPollingBot callback) {
        if (this.callback != null) {
            throw new InvalidParameterException("Callback has already been set");
        }
        this.callback = callback;
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    private class ReaderThread extends Thread implements UpdatesReader {

        private final UpdatesSupplier updatesSupplier;
        private final Object lock;
        private CloseableHttpClient httpclient;
        private BackOff backOff;
        private RequestConfig requestConfig;

        public ReaderThread(UpdatesSupplier updatesSupplier, Object lock) {
            this.updatesSupplier = Optional.ofNullable(updatesSupplier).orElse(this::getUpdatesFromServer);
            this.lock = lock;
        }

        @Override
        public synchronized void start() {
            httpclient = TelegramHttpClientBuilder.build(options);
            requestConfig = options.getRequestConfig();
            backOff = options.getBackOff();

            if (backOff == null) {
                backOff = new ExponentialBackOffFloor();
            }

            if (requestConfig == null) {
                requestConfig = RequestConfig.copy(RequestConfig.custom().build())
                        .setSocketTimeout(SOCKET_TIMEOUT)
                        .setConnectTimeout(SOCKET_TIMEOUT)
                        .setConnectionRequestTimeout(SOCKET_TIMEOUT).build();
            }

            super.start();
        }

        @Override
        public void interrupt() {
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (IOException e) {
                    log.error("interrupt() [{}] [{}]", e.getMessage(), e.getLocalizedMessage(), e.getCause());
                }
            }
            super.interrupt();
        }

        @Override
        public void run() {
            setPriority(Thread.MIN_PRIORITY);
            while (running.get()) {
                synchronized (lock) {
                    if (running.get()) {
                        try {
                            List<Update> updates = updatesSupplier.getUpdates();
                            if (updates.isEmpty()) {
                                lock.wait(500);
                            } else {
                                updates.removeIf(x -> x.getUpdateId() < lastReceivedUpdate);
                                lastReceivedUpdate = updates.parallelStream()
                                        .map(Update::getUpdateId)
                                        .max(Integer::compareTo)
                                        .orElse(0);
                                receivedUpdates.addAll(updates);

                                synchronized (receivedUpdates) {
                                    receivedUpdates.notifyAll();
                                }
                            }
                        } catch (InterruptedException e) {
                            if (!running.get()) {
                                receivedUpdates.clear();
                            }
                            log.warn("TelegramBot Run: msgInterruptedException before interrupt [{}] [{}]", e.getMessage(), e.getLocalizedMessage(), e.getCause());
                            interrupt();
                        } catch (Exception global) {
                            log.error("TelegramBot Run: msgException global [{}] [{}]", global.getMessage(), global.getLocalizedMessage(), global.getCause());
                            try {
                                synchronized (lock) {
                                    lock.wait(backOff.nextBackOffMillis());
                                }
                            } catch (InterruptedException e) {
                                if (!running.get()) {
                                    receivedUpdates.clear();
                                }
                                log.debug(e.getLocalizedMessage(), e);
                                interrupt();
                            }
                        }
                    }
                }
            }
            log.info("Reader thread has being closed");
        }

        private List<Update> getUpdatesFromServer() throws IOException {
            GetUpdates request = GetUpdates.builder()
                    .limit(options.getGetUpdatesLimit())
                    .timeout(options.getGetUpdatesTimeout())
                    .offset(lastReceivedUpdate + 1)
                    .build();

            if (options.getAllowedUpdates() != null) {
                request.setAllowedUpdates(options.getAllowedUpdates());
            }

            String url = options.getBaseUrl() + token + "/" + GetUpdates.PATH;
            HttpPost httpPost = new HttpPost(url);
            httpPost.addHeader("charset", StandardCharsets.UTF_8.name());
            httpPost.setConfig(requestConfig);
            httpPost.setEntity(new StringEntity(objectMapper.writeValueAsString(request), ContentType.APPLICATION_JSON));
            StatusLine statusLine = null;
            try (CloseableHttpResponse response = httpclient.execute(httpPost, options.getHttpContext())) {
                String responseContent = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() >= 500) {
                    log.warn(responseContent);
                    synchronized (lock) {
                        lock.wait(500);
                    }
                } else {
                    List<Update> updates = request.deserializeResponse(responseContent);
                    backOff.reset();
                    if (startConflict409.get() > 0) {
                        log.info("Finish reStart TelegramBot. Cnt409: [{}]", startConflict409.get());
                        startConflict409.set(0);
                    }
                    return updates;
                }
            } catch (SocketException | InvalidObjectException | TelegramApiRequestException e) {
                if (statusLine != null && statusLine.getStatusCode() == 409) {
                    if (startConflict409.get() == 0) {
                        log.error("ReStart TelegramBot: [{}]", statusLine);
                    }
                    startConflict409.incrementAndGet();
                } else {
                    log.error("getUpdatesFromServer msgTelegramApiRequestException... [{}] [{}]", e.getMessage(), e.getLocalizedMessage(), e.getCause());
                }
            } catch (SocketTimeoutException e) {
                log.error("getUpdatesFromServer SocketTimeoutException [{}] [{}]", e.getMessage(), e.getLocalizedMessage(), e.getCause());
            } catch (InterruptedException e) {
                log.error("getUpdatesFromServer InterruptedException [{}] [{}]", e.getMessage(), e.getLocalizedMessage(), e.getCause());
                interrupt();
            } catch (InternalError e) {
                if (e.getCause() instanceof InvocationTargetException) {
                    Throwable cause = e.getCause().getCause();
                    log.error("getUpdatesFromServer InternalError [{}] [{}]", cause.getMessage(), cause.getLocalizedMessage(), cause);
                } else throw e;
            }

            return Collections.emptyList();
        }
    }

    public interface UpdatesSupplier {
        List<Update> getUpdates() throws Exception;
    }

    private List<Update> getUpdateList() {
        List<Update> updates = new ArrayList<>();
        for (Iterator<Update> it = receivedUpdates.iterator(); it.hasNext();) {
            updates.add(it.next());
            it.remove();
        }
        return updates;
    }

    private class HandlerThread extends Thread implements UpdatesHandler {
        @Override
        public void run() {
            setPriority(Thread.MIN_PRIORITY);
            while (running.get()) {
                try {
                    List<Update> updates = getUpdateList();
                    if (updates.isEmpty()) {
                        synchronized (receivedUpdates) {
                            receivedUpdates.wait();
                            updates = getUpdateList();
                            if (updates.isEmpty()) {
                                continue;
                            }
                        }
                    }
                    callback.onUpdatesReceived(updates);
                } catch (InterruptedException e) {
                    log.debug(e.getLocalizedMessage(), e);
                    interrupt();
                } catch (Exception e) {
                    log.error(e.getLocalizedMessage(), e);
                }
            }
            log.debug("Handler thread has being closed");
        }
    }
}
