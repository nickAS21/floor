package org.nickas21.smart.tuya.event;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class MessageConfig {
    static AnnotationConfigApplicationContext ctx;

    @PostConstruct
    public void init() throws Exception {

    }

    @Configuration
    static class MessageConfigEvent {

    }
}
