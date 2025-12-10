package org.nickas21.smart.usr.io;

import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.usr.config.UsrTcpLogsWiFiProperties;
import org.nickas21.smart.usr.config.UsrTcpWiFiProperties;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Component
public class UsrTcpWiFiLogWriter implements Closeable {

    private final Map<Integer, BufferedWriter> todayWriters = new HashMap<>();
    private final Map<Integer, BufferedWriter> errorWriters = new HashMap<>();

    private String logDir;
    private UsrTcpWiFiProperties tcpProps;
    private UsrTcpLogsWiFiProperties tcpLogsProps;

    public void init(String logDir, UsrTcpWiFiProperties tcpProps, UsrTcpLogsWiFiProperties tcpLogsProps, Integer[] ports) {
        try {
            log.info("UsrTcpWiFiLogWriter init... logDir: {}", logDir);
            this.logDir = logDir;
            this.tcpProps = tcpProps;
            this.tcpLogsProps = tcpLogsProps;

            for (Integer port : ports) {
                todayWriters.put(port, openWriter(todayFileName(port)));
                errorWriters.put(port, openWriter(errorFileName(port)));
            }

        } catch (Exception e) {
            log.error("Failed to initialize writers", e);
            throw new RuntimeException(e);
        }
    }

    private String todayFileName(int port) {
        return tcpLogsProps.getTodayPrefix() + "_" + String.format("%02d", port) + ".log";
    }

    private String yesterdayFileName(int port) {
        return tcpLogsProps.getYesterdayPrefix() + "_" + String.format("%02d", port) + ".log";
    }

    private String errorFileName(int port) {
        return tcpLogsProps.getErrorPrefix() + "_" + String.format("%02d", port) + ".log";
    }

    private BufferedWriter openWriter(String filename) throws IOException {
        File file = Paths.get(this.logDir, filename).toFile();
        return new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8)
        );
    }

    // ---- WRITE TODAY ----
    public synchronized void writeToday(Integer port, UsrTcpWiFiPacketRecord rec) throws IOException {
        BufferedWriter writer = todayWriters.get(port);
        if (writer != null) {
            writer.write(rec.toLine());
            writer.flush();
        }
    }

    // ---- WRITE ERROR ----
    public synchronized void writeError(Integer port, UsrTcpWiFiPacketRecord rec) throws IOException {
        File file = Paths.get(logDir, errorFileName(port)).toFile();

        enforceErrorLogLimit(file);

        BufferedWriter writerError = errorWriters.get(port);
        if (writerError != null) {
            writerError.write(rec.toLine());
            writerError.flush();
        }
    }

    private void enforceErrorLogLimit(File file) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        if (lines.size() >= tcpLogsProps.getErrorLimit()) {
            lines.remove(0); // remove oldest
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        }
    }

    // ---- DAILY ROTATION ----
    public synchronized void rotateLogs() throws IOException {
        log.info("Rotating USR WiFi logs...");

        for (Integer port : todayWriters.keySet()) {
            // Close current writers
            todayWriters.get(port).close();

            // Rename today -> yesterday
            File today = Paths.get(logDir, todayFileName(port)).toFile();
            File yesterday = Paths.get(logDir, yesterdayFileName(port)).toFile();

            if (yesterday.exists()) yesterday.delete();
            if (today.exists()) today.renameTo(yesterday);

            // Create new today writer
            todayWriters.put(port, openWriter(todayFileName(port)));
        }

        log.info("Rotation complete.");
    }

    @Override
    public void close() {
        log.info("Closing all log writers...");

        // Функція для закриття колекції writer'ів та логування помилок
        Consumer<Map<Integer, BufferedWriter>> closeWriters = map -> {
            for (BufferedWriter writer : map.values()) {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        // Логуємо, але продовжуємо закривати інші writer'и
                        log.error("Error closing log writer", e);
                    }
                }
            }
            map.clear(); // Очищаємо мапу після закриття
        };

        closeWriters.accept(todayWriters);
        closeWriters.accept(errorWriters);

        log.info("All log writers successfully closed.");
    }
}
