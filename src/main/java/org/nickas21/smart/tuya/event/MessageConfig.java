package org.nickas21.smart.tuya.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
public class MessageConfig {
    static AnnotationConfigApplicationContext ctx;

    @PostConstruct
    public void init() throws Exception {

    }

    @Configuration
    static class MessageConfigEvent {

        @EventListener
        public void statusReportMessage(StatusReportMessage event) {
            log.info("### statusReport event happened, event: {}", event);
        }

    }
}
