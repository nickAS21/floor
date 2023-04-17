package org.nickas21.smart.tuya;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.tuya.event.BaseTuyaMessage;
import org.nickas21.smart.tuya.event.SourceMessage;
import org.nickas21.smart.tuya.mq.MessageFactory;
import org.nickas21.smart.tuya.mq.MessageVO;
import org.nickas21.smart.tuya.mq.MqPulsarConsumer;
import org.nickas21.smart.tuya.mq.TuyaConnectionMsg;
import org.nickas21.smart.tuya.mq.TuyaMessageUtil;
import org.nickas21.smart.tuya.mq.TuyaToken;
import org.nickas21.smart.tuya.constant.TuyaRegion;
import org.nickas21.smart.util.ConnectThreadFactory;
import org.nickas21.smart.util.JacksonUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.nickas21.smart.tuya.constant.EnvConstant.ENV_AK;
import static org.nickas21.smart.tuya.constant.EnvConstant.ENV_REGION;
import static org.nickas21.smart.tuya.constant.EnvConstant.ENV_SK;
import static org.nickas21.smart.tuya.constant.TuyaApi.envSystem;
import static org.nickas21.smart.util.JacksonUtil.OBJECT_MAPPER;

@Slf4j
@Service
public class TuyaConnection implements TuyaConnectionIn, ApplicationContextAware {
    private TuyaToken accessToken;
    private ExecutorService executor;
    private MqPulsarConsumer mqPulsarConsumer;
    private TuyaMessageDataSource connectionConfiguration;
    private static ApplicationContext ctx;

    /**
     * application.properties
     * connector.ak=
     * connector.sk=
     * connector.region=
     */
    @Value("${connector.region:}")
    public String region;

    @Value("${connector.ak:}")
    public String ak;

    @Value("${connector.sk:}")
    public String sk;

    @PostConstruct
    public void init() throws Exception {
        accessToken = null;
        executor = Executors.newSingleThreadExecutor(ConnectThreadFactory.forName(getClass().getSimpleName() + "-loop"));
        this.connectionConfiguration = getTuyaConnectionConfiguration();
        if (this.connectionConfiguration != null) {
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
            log.error("Input parameters error: \n- TuyaConnectionConfiguration: [null]. \n- ak: [{}] \n- sk: [{}] \n- region: [{}]", this.ak, this.sk, this.region);
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
            ctx.publishEvent(msg);
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
                        SourceMessage sourceMessage = JSON.parseObject(new String(incomingData.getData()), SourceMessage.class);


                        BaseTuyaMessage msg1 = MessageFactory.extract(sourceMessage, sk);
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

    private TuyaMessageDataSource getTuyaConnectionConfiguration() {

        try {
            String akConf = envSystem.get(ENV_AK);
            String skConf = envSystem.get(ENV_SK);
            String reConf = envSystem.get(ENV_REGION);

            TuyaRegion region = (reConf != null && reConf.isBlank()) ? TuyaRegion.valueOf(reConf) : null;
                        if (akConf == null || akConf.isEmpty()
                    || skConf == null || skConf.isEmpty() || region == null) {
                akConf = this.ak;
                skConf = this.sk;
                reConf = this.region;
                region = (reConf != null && !reConf.isEmpty()) ? TuyaRegion.valueOf(reConf) : null;
            }
            if (akConf != null && !akConf.isEmpty() && skConf != null && !skConf.isEmpty() && region != null) {
                String url = region.getMsgUrl();
                return TuyaMessageDataSource.builder()
                        .url(url)
                        .ak(akConf)
                        .sk(skConf)
                        .build();
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("During processing Tuya connection error.[{}]", e.getMessage());
            return null;
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.ctx = applicationContext;
    }
}
