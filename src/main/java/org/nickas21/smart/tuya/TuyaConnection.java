package org.nickas21.smart.tuya;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.tuya.mq.MessageVO;
import org.nickas21.smart.tuya.mq.MqPulsarConsumer;
import org.nickas21.smart.tuya.mq.TuyaConnectionMsg;
import org.nickas21.smart.tuya.mq.TuyaMessageUtil;
import org.nickas21.smart.tuya.service.TuyaDeviceService;
import org.nickas21.smart.tuya.source.ApiTuyaDataSource;
import org.nickas21.smart.tuya.source.TuyaMessageDataSource;
import org.nickas21.smart.util.ConnectThreadFactory;
import org.nickas21.smart.util.JacksonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
//public class TuyaConnection implements TuyaConnectionIn, ApplicationContextAware {
public class TuyaConnection implements TuyaConnectionIn {
    private ExecutorService executor;
    private MqPulsarConsumer mqPulsarConsumer;
    private TuyaMessageDataSource tuyaConnectionConfiguration;
//    private static ApplicationContext ctx;

    @Autowired
    private ApiTuyaDataSource tuyaDataSource;

    @Autowired()
    private TuyaDeviceService tuyaDeviceService;

    @PostConstruct
    public void init() throws Exception {
        executor = Executors.newSingleThreadExecutor(ConnectThreadFactory.forName(getClass().getSimpleName() + "-tuya"));
        tuyaDeviceService.setExecutorService(executor);
        this.tuyaConnectionConfiguration = tuyaDataSource.getTuyaConnectionConfiguration();
        if (this.tuyaConnectionConfiguration != null) {
            tuyaDeviceService.setConnectionConfiguration(this.tuyaConnectionConfiguration);
            mqPulsarConsumer = createMqConsumer(this.tuyaConnectionConfiguration.getAk(), this.tuyaConnectionConfiguration.getSk());
            mqPulsarConsumer.connect(false);
            this.executor.submit(() -> {
                try {
                    mqPulsarConsumer.start();
                } catch (Exception e) {
                    log.warn("During processing Tuya connection error caught!", e);
                }
            });
        } else {
            log.error("Input parameters error: \n- TuyaConnectionConfiguration: [null]. \n- ak: [{}] \n- sk: [{}] \n- region: [{}]", tuyaDataSource.tuyaAk, tuyaDataSource.tuyaSk, tuyaDataSource.tuyaRegion);
            this.destroy();
        }
    }

//    @Override
//    public void update(String accessId, String accessKey) throws Exception {
//
//    }

    @Override
    public void destroy() {
        if (mqPulsarConsumer != null) {
            try {
                mqPulsarConsumer.stop();
            } catch (Exception e) {
                log.error("Cannot stop message queue consumer!", e);
            }
        }
        if (executor != null) {
            List<Runnable> runnables = executor.shutdownNow();
            log.debug("Stopped executor service, list of returned runnables: {}", runnables);
        }
    }

    @Override
    public void process(TuyaConnectionMsg msg) {
        try {
            tuyaDeviceService.devicesFromUpDateStatusValue(msg);
         } catch (Exception e) {
            log.debug("Failed to apply data converter function: {}", e.getMessage(), e);
        }
    }

    private void resultHandler(String type, String msg, Exception exception) {
        if ("Tuya CONNECT".equals(type) && exception != null) {
            // Reconnect
            try {
                mqPulsarConsumer.stop();
            } catch (Exception ignored) {
            }
            this.executor.submit(() -> {
                try {
                    mqPulsarConsumer.start();
                } catch (Exception e) {
                    log.debug("During processing Tuya connection error caught!", e);
                }
            });

        }
        if (exception == null) {
            // Ok connect
            log.debug("Tuya Type: [{}], Status: [SUCCESS], msg: [{}]", type, msg);
            // Init devices and accessToken
            tuyaDeviceService.init();
        } else {
            log.error("Tuya Type: [{}], Status: [FAILURE], msg: [{}]", type, msg, exception);
        }
    }

    private MqPulsarConsumer createMqConsumer(String accessId, String accessKey) {
        return MqPulsarConsumer.builder()
                .serviceUrl(tuyaConnectionConfiguration.getRegion().getMsgUrl())
                .accessId(accessId)
                .accessKey(accessKey)
                .messageListener((incomingData) -> {
                    String decryptedData = "";
                    try {
                        MessageVO vo = JacksonUtil.fromBytes(incomingData.getData(), MessageVO.class);
                        if (vo != null) {

                            decryptedData = TuyaMessageUtil.decrypt(vo.getData(), accessKey.substring(8, 24));
                            JsonNode dataNode = JacksonUtil.fromString(decryptedData, JsonNode.class);
                            TuyaConnectionMsg msg = new TuyaConnectionMsg(dataNode);
                            this.process(msg);
                        }
                    } catch (IllegalArgumentException e) {
                        resultHandler("Input Decoder", decryptedData, e);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .resultHandler((this::resultHandler))
                .build();
    }
}

