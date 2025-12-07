package org.nickas21.smart.usr.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("org.nickas21.smart.usr")
@EnableConfigurationProperties(UsrTcpWiFiProperties.class)
public class UsrTcpWiFiConfig {}
