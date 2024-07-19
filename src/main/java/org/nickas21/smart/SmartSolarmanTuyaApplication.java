package org.nickas21.smart;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.TimeZone;

@Slf4j
@SpringBootApplication
public class SmartSolarmanTuyaApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Kyiv"));
        ConfigurableApplicationContext context = SpringApplication.run(SmartSolarmanTuyaApplication.class);

        // Add shutdown hook to the JVM
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("JVM Shutdown Hook: Cleaning up resources...");
            context.close(); // Ensure Spring context is closed
        }));
    }

}
