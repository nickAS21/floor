package org.nickas21.smart.tuya;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.tuya.mq.MessageVO;
import org.nickas21.smart.tuya.mq.MqPulsarConsumer;
import org.nickas21.smart.tuya.mq.TuyaConnectionMsg;
import org.nickas21.smart.tuya.mq.TuyaMessageUtil;
import org.nickas21.smart.tuya.mq.TuyaToken;
import org.nickas21.smart.util.ConnectThreadFactory;
import org.nickas21.smart.util.JacksonUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class TuyaConnection implements TuyaConnectionIn {
    private TuyaToken accessToken;
    private ExecutorService executor;
    private MqPulsarConsumer mqPulsarConsumer;
    private TuyaConnectionConfiguration connectionConfiguration;

    @Value("${connector.region:}")
    public String region;

    @Value("${connector.ak:}")
    public String ak;

    @Value("${connector.sk:}")
    public String sk;

    @Override
    public void init(TuyaConnectionConfiguration conf) throws Exception {
        accessToken = null;
        executor = Executors.newSingleThreadExecutor(ConnectThreadFactory.forName(getClass().getSimpleName() + "-loop"));
        connectionConfiguration = new TuyaConnectionConfiguration (conf.getAccessId(), conf.getAccessKey(), conf.getRegion());
        mqPulsarConsumer = createMqConsumer(conf.getAccessId(), conf.getAccessKey());
        mqPulsarConsumer.connect(false);
        this.executor.submit(() -> {
            try {
                mqPulsarConsumer.start();
            } catch (Exception e) {
                log.warn("During processing Tuya connection error caught!", e);
            }
        });
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

}
