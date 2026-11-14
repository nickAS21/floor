package org.nickas21.smart;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

import static org.nickas21.smart.util.LocationType.DACHA;

@Slf4j
@SpringBootApplication
@ConfigurationPropertiesScan("org.nickas21.smart")
@EnableScheduling
public class SmartSolarmanTuyaApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(DACHA.getZoneId()));
        ConfigurableApplicationContext context = SpringApplication.run(SmartSolarmanTuyaApplication.class);

        // Add shutdown hook to the JVM
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("JVM Shutdown Hook: Cleaning up resources...");
            context.close(); // Ensure Spring context is closed
        }));
    }

}
