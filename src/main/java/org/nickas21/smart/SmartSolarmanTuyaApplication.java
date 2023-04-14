package org.nickas21.smart;

import org.nickas21.smart.tuya.TuyaConnection;
import org.nickas21.smart.tuya.TuyaConnectionConfiguration;
import org.nickas21.smart.tuya.util.TuyaRegion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import static org.nickas21.smart.tuya.mq.TuyaHandler.getTuyaConnectionConfiguration;
import static org.nickas21.smart.tuya.util.TuyaRegion.EU;


@SpringBootApplication
public class SmartSolarmanTuyaApplication  implements CommandLineRunner {
    /**
     * connector.ak=
     * connector.sk=
     * connector.region=
     */
    private  String accessId = "3h8kqjefdt9saxksq8ak";
    private String accessKey = "8c095043fb1a4d8c9c642d63732a8cc6";
    private TuyaRegion region = EU;

    @Autowired
    private ApplicationContext applicationContext;


    public static void main(String[] args) {
        SpringApplication.run(SmartSolarmanTuyaApplication.class, args);


    }

    @Override
    public void run(String... args) throws Exception {
            TuyaConnection tuyaConnection = applicationContext.getBean(TuyaConnection.class);
            TuyaConnectionConfiguration conf = getTuyaConnectionConfiguration(tuyaConnection);
            tuyaConnection.init(conf);
    }
}
