package org.nickas21.smart.data.service.systemOut;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static org.nickas21.smart.util.LocationType.DACHA;

@Component
public class RuntimeLogBuffer {
    private static final int INTERNAL_MAX = 1000;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final LinkedList<String> lines = new LinkedList<>();
    private Path currentFile;
    private Path previousFile;
    private LocalDate currentDate;

    private String activeProgressLine = null;
    private String closedProgressPrefix = null;

    @Value("${usr.tcp.logs.dir:./logs/}")
    private String logsDir;

    @Value("${usr.tcp.logs.buffer-current-file:buffer_current.log}")
    private String currentFileName;

    @Value("${usr.tcp.logs.buffer-previous-file:buffer_previous.log}")
    private String previousFileName;

    @Value("${app.version:unknown}")
    private String version;

    private String buildStartBanner() {
        String timeStr = LocalTime.now(DACHA.getZoneId()).format(TIME_FMT);
        return "=== New Start [" + timeStr + "] -> ver: " + version + " ===\n";
    }

    @PostConstruct
    public synchronized void init() {
        try {
            Path dirPath = Paths.get(logsDir);
            Files.createDirectories(dirPath);

            currentFile = dirPath.resolve(currentFileName);
            previousFile = dirPath.resolve(previousFileName);
            currentDate = LocalDate.now(DACHA.getZoneId());

            String startBanner = buildStartBanner();

            if (Files.exists(currentFile)) {
                List<String> all = Files.readAllLines(currentFile, StandardCharsets.UTF_8);
                if (!all.isEmpty() && !all.get(0).trim().isEmpty()) {
                    LocalDate fileDate = LocalDate.parse(all.get(0).trim());
                    if (currentDate.isAfter(fileDate)) {
                        Files.move(currentFile, previousFile, StandardCopyOption.REPLACE_EXISTING);
                        createNewCurrentFile(currentDate, startBanner);
                    } else {
                        currentDate = fileDate;
                        List<String> entries = all.subList(1, all.size());
                        int start = Math.max(0, entries.size() - INTERNAL_MAX);
                        for (int i = start; i < entries.size(); i++) {
                            lines.add(entries.get(i) + "\n");
                        }
                        addLineToMemory(startBanner);
                        syncBufferToFile();
                    }
                } else {
                    createNewCurrentFile(currentDate, startBanner);
                }
            } else {
                createNewCurrentFile(currentDate, startBanner);
            }
        } catch (Exception e) {
            lines.add("RuntimeLogBuffer init error: " + e.getMessage() + "\n");
        }
    }

    @PreDestroy
    public synchronized void onShutdown() {
        finishProgress("Shutdown");
    }

    public synchronized void append(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        String cleanText = text
                .replaceAll("\u001B\\[[;\\d]*m", "")
                .replace('\u202F', ' ')
                .replace('\u00A0', ' ');

        if (cleanText.endsWith("\r")) {
            String cleanLine = cleanText.substring(0, cleanText.length() - 1) + "\n";
            String prefix = extractProgressPrefix(cleanLine);

            if (prefix.equals(closedProgressPrefix)) {
                return;
            }
            closedProgressPrefix = null;

            if (activeProgressLine != null && !prefix.equals(extractProgressPrefix(activeProgressLine))) {
                finishProgress("Interrupted");
            }

            if (activeProgressLine != null) {
                lines.removeLastOccurrence(activeProgressLine);
            }
            addLineToMemory(cleanLine);
            activeProgressLine = cleanLine;
            syncBufferToFile();
            return;
        }

        if (cleanText.startsWith("|===")) {
            finishProgress("Finish");
            return;
        }

        String normalized = cleanText.endsWith("\n") ? cleanText : cleanText + "\n";
        checkDateRotation(normalized);
        addLineToMemory(normalized);
        syncBufferToFile();
    }

    public synchronized void finishProgress() {
        finishProgress("Shutdown");
    }

    public synchronized void finishProgress(String reason) {
        if (activeProgressLine != null) {
            closedProgressPrefix = extractProgressPrefix(activeProgressLine);
            String timeStr = LocalTime.now(DACHA.getZoneId()).format(TIME_FMT);
            String finalLine = "[" + reason + " " + timeStr + "] " + activeProgressLine;
            lines.removeLastOccurrence(activeProgressLine);
            addLineToMemory(finalLine);
            activeProgressLine = null;
            syncBufferToFile();
        }
    }

    private void syncBufferToFile() {
        if (currentFile == null) {
            return;
        }
        try {
            String content = currentDate + "\n" + String.join("", lines);
            Files.writeString(currentFile, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            lines.add("RuntimeLogBuffer file write error: " + e.getMessage() + "\n");
        }
    }

    private void checkDateRotation(String text) {
        LocalDate logDate = extractIsoDate(text);
        if (logDate != null && logDate.isAfter(currentDate)) {
            try {
                if (Files.exists(currentFile)) {
                    Files.move(currentFile, previousFile, StandardCopyOption.REPLACE_EXISTING);
                }
                currentDate = logDate;
                lines.clear();
            } catch (IOException ignored) {
            }
        }
    }

    private void createNewCurrentFile(LocalDate date, String firstEntry) throws IOException {
        currentDate = date;
        lines.clear();
        if (firstEntry != null) {
            addLineToMemory(firstEntry);
        }
        syncBufferToFile();
    }

    private String extractProgressPrefix(String line) {
        int idx = line.indexOf("[=");
        return idx > 0 ? line.substring(0, idx) : line;
    }

    private void addLineToMemory(String line) {
        lines.add(line);
        if (lines.size() > INTERNAL_MAX) {
            lines.removeFirst();
        }
    }

    private LocalDate extractIsoDate(String text) {
        if (text.length() < 29) {
            return null;
        }
        int searchLimit = Math.min(text.length() - 29, 80);
        String curYearPrefix = currentDate.getYear() + "-";
        String nextYearPrefix = (currentDate.getYear() + 1) + "-";

        for (int i = 0; i <= searchLimit; i++) {
            if (text.startsWith(curYearPrefix, i) || text.startsWith(nextYearPrefix, i)) {
                try {
                    return OffsetDateTime.parse(text.substring(i, i + 29)).toLocalDate();
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    public synchronized String get(int limit) {
        int count = Math.min(lines.size(), limit);
        return lines.stream()
                .skip(lines.size() - count)
                .collect(Collectors.joining());
    }

    public synchronized String getCurrentLogs() {
        return readLogsFromFile(currentFile);
    }

    public synchronized String getPreviousLogs() {
        return readLogsFromFile(previousFile);
    }

    private String readLogsFromFile(Path file) {
        if (file == null || !Files.exists(file)) {
            return "";
        }
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            int firstNewLine = content.indexOf('\n');
            return firstNewLine >= 0 ? content.substring(firstNewLine + 1) : "";
        } catch (IOException e) {
            return "";
        }
    }
}