package org.nickas21.smart.data.service.systemOut;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class MemoryLogAppender extends AppenderBase<ILoggingEvent> {

    private static RuntimeLogBuffer buffer;

    public static void init(RuntimeLogBuffer b) {
        buffer = b;
    }

    @Override
    protected void append(ILoggingEvent e) {
        if (buffer == null) return;

        String line =
                "%s  %-5s %s --- [%s] %s : %s%n".formatted(
                        Instant.ofEpochMilli(e.getTimeStamp()),
                        e.getLevel(),
                        e.getThreadName(),
                        e.getLoggerName(),
                        e.getLoggerName(),
                        e.getFormattedMessage()
                );

        buffer.append(line);
    }
}
