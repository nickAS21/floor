package org.nickas21.smart.solarman;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.nickas21.smart.solarman.service.DefaultSolarmanInverterService;
import org.nickas21.smart.solarman.source.ApiSolarmanDataSource;
import org.nickas21.smart.solarman.source.SolarmanMqttDataSource;
import org.nickas21.smart.util.ConnectThreadFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class SolarmanMqttConnection {
    private ExecutorService executor;
    private SolarmanMqttDataSource solarmanMqttDataConnection;
    public MqttAsyncClient client;

    /**
     *
     def connect_mqtt(broker, port, username, password):
     """
     Create an MQTT connection
     :param broker: MQTT broker
     :param port: MQTT broker port
     :param username: MQTT username
     :param password: MQTT password
     :return:
     """
     client_id = f'solarmanpv-mqtt-{random.randint(0, 1000)}'
     client = mqtt_client.Client(client_id)
     client.username_pw_set(username, password)
     client.connect(broker, port)
     return client
     */


    @Autowired
    private ApiSolarmanDataSource solarmanDataSource;

    @Autowired
    private DefaultSolarmanInverterService solarmanInverterService;

    @PostConstruct
    public void init() throws Exception {
        this.executor = Executors.newSingleThreadExecutor(ConnectThreadFactory.forName(getClass().getSimpleName() + "-solarman"));
        this.solarmanMqttDataConnection = solarmanDataSource.getSolarmanMqttDataSource();
        this.solarmanInverterService.setExecutorService(this.executor);
        this.solarmanInverterService.setSolarmanMqttDataSource(this.solarmanMqttDataConnection);
        this.solarmanInverterService.init();
//        createClient();

    }


    private void createClient() throws MqttException {
        this.client = new MqttAsyncClient("tcp://" + this.solarmanMqttDataConnection.getRegion() + ":1883", this.solarmanMqttDataConnection.getLoggerId(), new MemoryPersistence());
        this.client.setCallback(new SolarmanMqttCallback());
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setUserName(this.solarmanMqttDataConnection.getUserName());
        options.setPassword(this.solarmanMqttDataConnection.getPassWord().getBytes(StandardCharsets.UTF_8));
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(30);
        options.setKeepAliveInterval(30);
        client.connect(options);
        if (client.isConnected()) {
            log.info("isConnect");
        } else {
            log.error("is not Connect");
        }
    }
}
