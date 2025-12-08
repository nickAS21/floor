package org.nickas21.smart.usr.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.usr.io.UsrTcpWiFiLogWriter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UsrTcpRotationScheduler {

    private final UsrTcpWiFiLogWriter writer;

    @Scheduled(cron = "${usr.tcp.logs.rotation-cron}") // every day at 01:00
    public void rotate() {
        try {
            writer.rotateLogs();
        } catch (Exception e) {
            log.error("Rotation failed", e);
        }
    }
}