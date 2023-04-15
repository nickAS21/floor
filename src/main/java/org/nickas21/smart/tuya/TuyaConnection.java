package org.nickas21.smart.tuya;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.tuya.mq.MessageVO;
import org.nickas21.smart.tuya.mq.MqPulsarConsumer;
import org.nickas21.smart.tuya.mq.TuyaConnectionMsg;
import org.nickas21.smart.tuya.mq.TuyaMessageUtil;
import org.nickas21.smart.tuya.mq.TuyaToken;
import org.nickas21.smart.tuya.util.TuyaRegion;
import org.nickas21.smart.util.ConnectThreadFactory;
import org.nickas21.smart.util.JacksonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.nickas21.smart.tuya.mq.TuyaHandler.CONNECTOR_AK;
import static org.nickas21.smart.tuya.mq.TuyaHandler.CONNECTOR_RE;
import static org.nickas21.smart.tuya.mq.TuyaHandler.CONNECTOR_SK;
import static org.nickas21.smart.tuya.mq.TuyaHandler.envSystem;

@Slf4j
@Service
public class TuyaConnection implements TuyaConnectionIn {
    private TuyaToken accessToken;
    private ExecutorService executor;
    private MqPulsarConsumer mqPulsarConsumer;
    private TuyaConnectionConfiguration connectionConfiguration;

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
        TuyaConnectionConfiguration conf = getTuyaConnectionConfiguration();
        if (conf != null) {
            connectionConfiguration = new TuyaConnectionConfiguration(conf.getAccessId(), conf.getAccessKey(), conf.getRegion());
            mqPulsarConsumer = createMqConsumer(conf.getAccessId(), conf.getAccessKey());
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

    }

    @Override
    public void process(TuyaConnectionMsg msg) {

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
                .serviceUrl(connectionConfiguration.getRegion().getMsgUrl())
                .accessId(accessId)
                .accessKey(accessKey)
                .messageListener((incomingData) -> {
                    MessageVO vo = JacksonUtil.fromBytes(incomingData.getData(), MessageVO.class);
                    if (vo != null) {
                        String decryptedData = "";
                        try {
                            decryptedData = TuyaMessageUtil.decrypt(vo.getData(), accessKey.substring(8, 24));
                            JsonNode dataNode = JacksonUtil.fromString(decryptedData, JsonNode.class);
                            TuyaConnectionMsg msg = new TuyaConnectionMsg(dataNode);
                            this.process(msg);
                        } catch (Exception e) {
                            resultHandler("Input Decoder", decryptedData, e);
                        }
                    }
                })
                .resultHandler((this::resultHandler))
                .build();
    }

    private TuyaConnectionConfiguration getTuyaConnectionConfiguration() {
        try {
            String akConf = envSystem.get(CONNECTOR_AK);
            String skConf = envSystem.get(CONNECTOR_SK);
            String reConf = envSystem.get(CONNECTOR_RE);
            TuyaRegion region = (reConf != null && reConf.isBlank()) ? TuyaRegion.valueOf(reConf) : null;
            if (akConf == null || akConf.isEmpty()
                    || skConf == null || skConf.isEmpty() || region == null) {
                akConf = this.ak;
                skConf = this.sk;
                reConf = this.region;
                region = (reConf != null && !reConf.isEmpty()) ? TuyaRegion.valueOf(reConf) : null;
            }
            if (akConf != null && !akConf.isEmpty() && skConf != null && !skConf.isEmpty() && region != null) {
                return new TuyaConnectionConfiguration(akConf, skConf, region);
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("During processing Tuya connection error.[{}]", e.getMessage());
            return null;
        }
    }

}
