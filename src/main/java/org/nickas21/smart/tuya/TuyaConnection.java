package org.nickas21.smart.tuya;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.tuya.mq.MessageVO;
import org.nickas21.smart.tuya.mq.MqPulsarConsumer;
import org.nickas21.smart.tuya.mq.TuyaConnectionMsg;
import org.nickas21.smart.tuya.mq.TuyaMessageUtil;
import org.nickas21.smart.tuya.service.TuDeviceService;
import org.nickas21.smart.tuya.sourece.ApiDataSource;
import org.nickas21.smart.util.ConnectThreadFactory;
import org.nickas21.smart.util.JacksonUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.nickas21.smart.util.JacksonUtil.OBJECT_MAPPER;

@Slf4j
@Service
public class TuyaConnection implements TuyaConnectionIn, ApplicationContextAware {
    private ExecutorService executor;
    private MqPulsarConsumer mqPulsarConsumer;
    private TuyaMessageDataSource connectionConfiguration;
    private static ApplicationContext ctx;

    @Autowired
    private ApiDataSource dataSource;

    @Autowired(required=true)
    private TuDeviceService deviceService;

    @PostConstruct
    public void init() throws Exception {
        executor = Executors.newSingleThreadExecutor(ConnectThreadFactory.forName(getClass().getSimpleName() + "-loop"));
        deviceService.setExecutorService(executor);
        this.connectionConfiguration = dataSource.getTuyaConnectionConfiguration();
        if (this.connectionConfiguration != null) {
            deviceService.setConnectionConfiguration(this.connectionConfiguration);
            mqPulsarConsumer = createMqConsumer(this.connectionConfiguration.getAk(), this.connectionConfiguration.getSk());
            mqPulsarConsumer.connect(false);
            this.executor.submit(() -> {
                try {
                    mqPulsarConsumer.start();
                } catch (Exception e) {
                    log.warn("During processing Tuya connection error caught!", e);
                }
            });
        } else {
            log.error("Input parameters error: \n- TuyaConnectionConfiguration: [null]. \n- ak: [{}] \n- sk: [{}] \n- region: [{}]", dataSource.ak, dataSource.sk, dataSource.region);
        }
    }

    @Override
    public void update(String accessId, String accessKey) throws Exception {

    }

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
            byte[] data = OBJECT_MAPPER.writeValueAsBytes(msg.getJson());
            log.info("devId: [{}], status: [{}] -> [{}]", msg.getJson().get("devId").asText(), msg.getJson().get("status").get(0).get("code"), msg.getJson().get("status").get(0).get("value"));
//            ctx.publishEvent(msg);
        } catch (Exception e) {
            log.debug("Failed to apply data converter function: {}", e.getMessage(), e);
        }
    }

    private void resultHandler(String type, String msg, Exception exception) {
        if ("CONNECT".equals(type) && exception != null) {
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
            log.debug("Type: [{}], Status: [SUCCESS], msg: [{}]", type, msg);
        } else {
            log.error("Type: [{}], Status: [FAILURE], msg: [{}]", type, msg, exception);
        }
    }

    private MqPulsarConsumer createMqConsumer(String accessId, String accessKey) {
        return MqPulsarConsumer.builder()
                .serviceUrl(connectionConfiguration.getUrl())
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

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.ctx = applicationContext;
    }
}
