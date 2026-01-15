package org.nickas21.smart.data.service;

import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.DefaultSmartSolarmanTuyaService;
import org.nickas21.smart.data.dataEntityDto.BatteryInfoDto;
import org.nickas21.smart.data.dataEntityDto.DataHomeDto;
import org.nickas21.smart.data.dataEntityDto.HistoryDto;
import org.nickas21.smart.tuya.TuyaDeviceService;
import org.nickas21.smart.usr.config.UsrTcpLogsWiFiProperties;
import org.nickas21.smart.usr.io.UsrTcpWiFiLogWriter;
import org.nickas21.smart.usr.service.UsrTcpWiFiParseData;
import org.nickas21.smart.usr.service.UsrTcpWiFiService;
import org.nickas21.smart.util.LocationType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.nickas21.smart.usr.io.UsrTcpWiFiLogWriter.fileNameInfo;
import static org.nickas21.smart.util.JacksonUtil.fromString;
import static org.nickas21.smart.util.LocationType.DACHA;
import static org.nickas21.smart.util.LocationType.GOLEGO;

@Slf4j
@Service
public class HistoryService {

    private final DefaultSmartSolarmanTuyaService solarmanTuyaService;
    private final TuyaDeviceService deviceService;
    private final UsrTcpWiFiParseData usrTcpWiFiParseData;
    private final UsrTcpWiFiService usrTcpWiFiService;
    public final UsrTcpLogsWiFiProperties usrTcpLogsWiFiProperties;
    private final UsrTcpWiFiLogWriter logWriter;
    private final DataUnitService unitService;

    public HistoryService(DefaultSmartSolarmanTuyaService solarmanTuyaService, TuyaDeviceService deviceService,
                          UsrTcpWiFiParseData usrTcpWiFiParseData, UsrTcpWiFiService usrTcpWiFiService,
                          UsrTcpLogsWiFiProperties usrTcpLogsWiFiProperties, UsrTcpWiFiLogWriter logWriter, DataUnitService unitService) {
        this.solarmanTuyaService = solarmanTuyaService;
        this.deviceService = deviceService;
        this.usrTcpWiFiParseData = usrTcpWiFiParseData;
        this.usrTcpWiFiService = usrTcpWiFiService;
        this.usrTcpLogsWiFiProperties = usrTcpLogsWiFiProperties;
        this.logWriter = logWriter;
        this.unitService = unitService;
    }

    private List<HistoryDto> readHistoryFromFile(LocationType location, String prefix) {
        String logDir = usrTcpWiFiService.getLogsDir();
        String fileName = fileNameInfo(location, prefix);
        File file = Paths.get(logDir, fileName).toFile();

        // Якщо файлу немає або він порожній — відразу повертаємо порожній список
        if (!file.exists() || file.length() == 0) {
            return new ArrayList<>();
        }

        try (Stream<String> lines = Files.lines(file.toPath(), StandardCharsets.UTF_8)) {
            return lines
                    .filter(line -> line != null && !line.isBlank()) // Ігноруємо порожні рядки
                    .map(line -> {
                        try {
                            return fromString(line, HistoryDto.class);
                        } catch (Exception e) {
                            log.error("Error parsing history line in {}: {}", fileName, line);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull) // Видаляємо null-результати
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to read history file: {}", fileName, e);
            return new ArrayList<>();
        }
    }

    public List<HistoryDto> getHistoryGolegoToday() {
        return readHistoryFromFile(LocationType.GOLEGO, usrTcpLogsWiFiProperties.getTodayPrefix());
    }

    public List<HistoryDto> getHistoryGolegoYesterday() {
        return readHistoryFromFile(LocationType.GOLEGO, usrTcpLogsWiFiProperties.getYesterdayPrefix());
    }

    public List<HistoryDto> getHistoryDachaToday() {
        return readHistoryFromFile(LocationType.DACHA, usrTcpLogsWiFiProperties.getTodayPrefix());
    }

    public List<HistoryDto> getHistoryDachaYesterday() {
        return readHistoryFromFile(LocationType.DACHA, usrTcpLogsWiFiProperties.getYesterdayPrefix());
    }

    @Scheduled(fixedRateString = "${usr.tcp.logs.writeInterval:480000}") // Кожні 8 хв
    public void processLogs() {
        log.info("Starting scheduled log processing for Golego and Dacha...");

        // 1. Запис для DACHA (8 хв)
        try {
            DataHomeDto dachaData = new DataHomeDto(solarmanTuyaService, deviceService, usrTcpWiFiService);
            List<BatteryInfoDto> batteries = this.unitService.getBatteries (DACHA);
            logWriter.writeToday(LocationType.DACHA, new HistoryDto(dachaData, batteries));
        } catch (Exception e) {
            log.error("Error writing Dacha logs", e);
        }

        try {
            // Тепер Golego пишеться з тим самим ритмом, що і Dacha
            DataHomeDto golegoData = new DataHomeDto(deviceService, usrTcpWiFiParseData, usrTcpWiFiService);
            List<BatteryInfoDto> batteries = this.unitService.getBatteries (GOLEGO);
            logWriter.writeToday(LocationType.GOLEGO, new HistoryDto(golegoData, batteries));
        } catch (Exception e) {
            log.error("Error writing Golego logs", e);
        }
    }
}


