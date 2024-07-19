package org.nickas21.smart;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;

@Slf4j
@Component
public class GracefulShutdownHook implements ApplicationListener<ContextClosedEvent> {

    @Override
    public void onApplicationEvent(@NonNull ContextClosedEvent event) {
        log.info("Context Closed Event received, performing graceful shutdown...");

        // Perform your cleanup tasks here
        try {
            // Simulate cleanup task
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            log.error("Shutdown interrupted", e);
        }

        log.info("Cleanup completed, application shutting down.");
    }
}

