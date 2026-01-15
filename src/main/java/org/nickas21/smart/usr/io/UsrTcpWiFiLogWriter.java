package org.nickas21.smart.usr.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.data.dataEntityDto.ErrorInfoDto;
import org.nickas21.smart.data.dataEntityDto.HistoryDto;
import org.nickas21.smart.usr.config.UsrTcpLogsWiFiProperties;
import org.nickas21.smart.util.LocationType;
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

@Slf4j
@Component
public class UsrTcpWiFiLogWriter implements Closeable {

    private final Map<LocationType, BufferedWriter> todayWriters = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String logDir;
    private UsrTcpLogsWiFiProperties tcpLogsProps;

    public void init(String logDir, UsrTcpLogsWiFiProperties tcpLogsProps) {
        this.logDir = logDir;
        this.tcpLogsProps = tcpLogsProps;
        try {
            for (LocationType locationType : LocationType.values()) {
                todayWriters.put(locationType, openWriter(fileNameInfo(locationType, tcpLogsProps.getTodayPrefix())));
            }
        } catch (IOException e) {
            log.error("Init writers failed", e);
        }
    }

    public static String fileNameInfo(LocationType location, String prefix) {
        return prefix + location.getNameForFile() + ".jsonl";
    }

    private BufferedWriter openWriter(String filename) throws IOException {
        File file = Paths.get(this.logDir, filename).toFile();
        return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8));
    }

    // Запис логів Today (HistoryDto)
    public synchronized void writeToday(LocationType locationType, HistoryDto rec) throws IOException {
        BufferedWriter writer = todayWriters.get(locationType);
        if (writer != null) {
            writer.write(objectMapper.writeValueAsString(rec));
            writer.newLine();
            writer.flush(); // Захист від втрати даних при відключенні світла
        }
    }

    // Оптимізований запис Error - тільки APPEND
    public synchronized void writeError(LocationType locationType, ErrorInfoDto rec) throws IOException {
        String filename = fileNameInfo(locationType, tcpLogsProps.getErrorPrefix());
        File file = Paths.get(logDir, filename).toFile();
        try (BufferedWriter errWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8))) {
            errWriter.write(objectMapper.writeValueAsString(rec));
            errWriter.newLine();
        }
    }

    // Ротація та підрізання файлів помилок
    public synchronized void rotateLogs() throws IOException {
        for (LocationType loc : LocationType.values()) {
            // 1. Закриваємо поточний Today
            if (todayWriters.containsKey(loc)) todayWriters.get(loc).close();

            // 2. Ротація Today -> Yesterday
            File today = Paths.get(logDir, fileNameInfo(loc, tcpLogsProps.getTodayPrefix())).toFile();
            File yesterday = Paths.get(logDir, fileNameInfo(loc, tcpLogsProps.getYesterdayPrefix())).toFile();
            if (yesterday.exists()) yesterday.delete();
            if (today.exists()) today.renameTo(yesterday);

            // 3. Відкриваємо новий Today
            todayWriters.put(loc, openWriter(fileNameInfo(loc, tcpLogsProps.getTodayPrefix())));

            // 4. "Підрізаємо" Error файл за лімітом рядків
            enforceErrorLogLimit(loc);
        }
    }

    private void enforceErrorLogLimit(LocationType loc) throws IOException {
        File file = Paths.get(logDir, fileNameInfo(loc, tcpLogsProps.getErrorPrefix())).toFile();
        if (!file.exists()) return;
        List<String> lines = Files.readAllLines(file.toPath());
        if (lines.size() > tcpLogsProps.getErrorLimit()) {
            List<String> trimmed = lines.subList(lines.size() - tcpLogsProps.getErrorLimit(), lines.size());
            Files.write(file.toPath(), trimmed, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    @Override
    public void close() throws IOException {
        for (BufferedWriter w : todayWriters.values()) if (w != null) w.close();
    }
}