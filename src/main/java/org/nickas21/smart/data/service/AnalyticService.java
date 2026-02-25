package org.nickas21.smart.data.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.data.dataEntityDto.DataAnalyticDto;
import org.nickas21.smart.data.dataEntityDto.DataErrorInfoDto;
import org.nickas21.smart.data.dataEntityDto.PowerType;
import org.nickas21.smart.tuya.TuyaDeviceService;
import org.nickas21.smart.usr.entity.InverterData;
import org.nickas21.smart.usr.entity.InvertorGolegoData90;
import org.nickas21.smart.usr.service.UsrTcpWiFiBatteryRegistry;
import org.nickas21.smart.usr.service.UsrTcpWiFiParseData;
import org.nickas21.smart.util.LocationType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.nickas21.smart.data.dataEntityDto.DataHomeDto.golegoInverterPowerDefault;

@Slf4j
@Service
public class AnalyticService {

    @Value("${smart.analytic.dir:./usrAnalytic/}")
    private String dirAnalytic;

    @Value("${smart.analytic.file-prefix:analytic_}")
    private String filePrefix;

    @Value("${smart.analytic.zones.day-start}")
    private String dayStartStr;

    @Value("${smart.analytic.zones.night-start}")
    private String nightStartStr;

    @Value("${smart.analytic.golego.update-rate:300000}")
    private long updateRateMs;

    private final Map<String, DataAnalyticDto> analyticCache = new ConcurrentHashMap<>();

    private final UsrTcpWiFiParseData usrTcpWiFiParseData;
    private final TuyaDeviceService deviceService;

    private DataAnalyticDto currentDayGolego;
    public static final String patternMonthFile = "yyyy-MM";
    public static final String patternDayKey = "yyyy-MM-dd";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        this.currentDayGolego = loadDtoForDate(LocalDate.now(), LocationType.GOLEGO, PowerType.GRID);
    }

    public AnalyticService(UsrTcpWiFiParseData usrTcpWiFiParseData, TuyaDeviceService deviceService) {
        this.usrTcpWiFiParseData = usrTcpWiFiParseData;
        this.deviceService = deviceService;
    }

    @Scheduled(fixedRateString = "${smart.analytic.golego.update-rate:300000}")
    public void updateAndSaveGolegoAnalytic() {
        double currentGridPower = 0;
        UsrTcpWiFiBatteryRegistry usrTcpWiFiBatteryRegistry = usrTcpWiFiParseData.getUsrTcpWiFiBatteryRegistry();
        Integer portInverterGolego = usrTcpWiFiParseData.getUsrTcpWiFiProperties().getPortInverterGolego();
        InverterData inverterDataGolego = usrTcpWiFiBatteryRegistry.getInverter(portInverterGolego);
        Boolean gridRelayCodeGolegoStateOnLine = deviceService.getGridRelayCodeGolegoStateOnLine();
        Boolean gridRelayCodeGolegoStateSwitch =  deviceService.getGridRelayCodeGolegoStateSwitch();
        if (gridRelayCodeGolegoStateSwitch != null && gridRelayCodeGolegoStateSwitch
            && gridRelayCodeGolegoStateOnLine != null && gridRelayCodeGolegoStateOnLine) {
            if (inverterDataGolego != null && inverterDataGolego.getInvertorGolegoData90() != null && inverterDataGolego.getInvertorGolegoData90().getHexMap().length > 0) {
                InvertorGolegoData90 invertorGolegoData90 = inverterDataGolego.getInvertorGolegoData90();
                double batteryVol = invertorGolegoData90.getBatteryVoltage();
                double batteryCurrent = invertorGolegoData90.getBatteryCurrent();
                double homePower = invertorGolegoData90.getLoadOutputActivePower();
                currentGridPower = batteryVol * batteryCurrent + homePower + golegoInverterPowerDefault;
            }

        }

        // 2. Рахуємо енергію за інтервал (кВт·год)
        double deltaKwh = (currentGridPower / 1000.0) * (updateRateMs / 3600000.0);

        // 3. Плюсуємо в T1 або T2 залежно від isDayZone(timestamp)
        updateStorage(deltaKwh, System.currentTimeMillis(), LocationType.GOLEGO, PowerType.GRID);
    }

    public List<DataErrorInfoDto> getGolegoAnalyticDayCurrent() {
        List<DataErrorInfoDto> errorInfoDtos = new ArrayList<>();
        // read from file GolegoErrors
//        historyGolegoaDtos.add(new  DataHistoryDto());
        return errorInfoDtos;
    }

    public List<DataErrorInfoDto> getDachaAnalyticDayCurrent() {
        List<DataErrorInfoDto> dataErrorInfoDtos = new ArrayList<>();
        // read from file DachaErrors
//        historyGolegoaDtos.add(new  DataHistoryDto());
        return dataErrorInfoDtos;
    }

    public List<DataErrorInfoDto> getGolegoAnalyticPeriod() {
        List<DataErrorInfoDto> errorInfoDtos = new ArrayList<>();
        // read from file GolegoErrors
//        historyGolegoaDtos.add(new  DataHistoryDto());
        return errorInfoDtos;
    }

    public List<DataErrorInfoDto> getDachaAnalyticPeriod() {
        List<DataErrorInfoDto> dataErrorInfoDtos = new ArrayList<>();
        // read from file DachaErrors
//        historyGolegoaDtos.add(new  DataHistoryDto());
        return dataErrorInfoDtos;
    }

    public void updateStorage(double deltaKwh, long timestamp, LocationType location, PowerType type) {
        // 1. Визначаємо, який об'єкт оновлювати
        DataAnalyticDto currentDto;

        if (location == LocationType.GOLEGO) {
            currentDto = this.currentDayGolego;
        } else {
            // Для Dacha логіка буде аналогічною, коли додамо currentDayDacha
            currentDto = loadDtoForDate(LocalDate.now(), location, type);
        }

        if (currentDto == null) {
            log.error("Критична помилка: об'єкт аналітики не ініціалізовано для {}", location);
            return;
        }

        // 2. Розподіляємо дельту по зонах
        if (isDayZone(timestamp)) {
            currentDto.setPowerDay(currentDto.getPowerDay() + deltaKwh);
        } else {
            currentDto.setPowerNight(currentDto.getPowerNight() + deltaKwh);
        }

        // 3. Оновлюємо загальні поля
        currentDto.setPowerTotal(currentDto.getPowerDay() + currentDto.getPowerNight());
        currentDto.setTimestamp(timestamp);

        // 4. Синхронізуємо з диском
        saveToMonthlyFile(currentDto);
    }

    private boolean isDayZone(long timestamp) {
        LocalTime time = Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalTime();

        return !time.isBefore(LocalTime.parse(dayStartStr))
                && time.isBefore(LocalTime.parse(nightStartStr));
    }

    private synchronized void saveToMonthlyFile(DataAnalyticDto dto) {
        // 1. Формуємо ім'я файлу (напр. analytic_2026-02.json)
        String monthSuffix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        Path path = Paths.get(dirAnalytic, filePrefix + monthSuffix + ".json");

        // 2. Формуємо ключ для мапи (напр. 2026-02-25_GOLEGO_GRID)
        String mapKey = LocalDate.now().toString() + "_" + dto.getLocation() + "_" + dto.getPowerType();

        try {
            Map<String, DataAnalyticDto> monthData;

            // 3. Читаємо існуючі дані, щоб не затерти інші дні місяця
            if (Files.exists(path)) {
                String json = Files.readString(path, StandardCharsets.UTF_8);
                monthData = objectMapper.readValue(json,
                        new TypeReference<ConcurrentHashMap<String, DataAnalyticDto>>() {});
            } else {
                // Якщо файлу ще немає (новий місяць), створюємо директорію та нову мапу
                Files.createDirectories(path.getParent());
                monthData = new ConcurrentHashMap<>();
            }

            // 4. Оновлюємо/додаємо наш DTO в мапу місяця
            monthData.put(mapKey, dto);

            // 5. Записуємо оновлену мапу назад у файл
            String resultJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(monthData);
            Files.writeString(path, resultJson, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            log.info("Аналітику збережено в {}. Ключ: {}, Total: {} kWh",
                    path.getFileName(), mapKey, dto.getPowerTotal());

        } catch (IOException e) {
            log.error("Помилка запису аналітики: ", e);
        }
    }
    private DataAnalyticDto loadDtoForDate(LocalDate date, LocationType location, PowerType powerType) {
        // 1. Формуємо шляхи та ключі
        String monthSuffix = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String mapKey = date.toString() + "_" + location + "_" + powerType;
        Path path = Paths.get(dirAnalytic, filePrefix + monthSuffix + ".json");

        // 2. Якщо файлу немає — одразу новий об'єкт
        if (!Files.exists(path)) {
            log.info("Файл {} не знайдено, створюємо новий DTO", path.getFileName());
            return new DataAnalyticDto(location, powerType);
        }

        try {
            // 3. Читаємо весь файл
            String json = Files.readString(path, StandardCharsets.UTF_8);
            if (json == null || json.isBlank()) return new DataAnalyticDto(location, powerType);;

            // 4. Десеріалізуємо в мапу (використовуємо JacksonUtil, якщо він вміє в TypeReference, або стандартний ObjectMapper)
            Map<String, DataAnalyticDto> monthData = objectMapper.readValue(json,
                    new TypeReference<ConcurrentHashMap<String, DataAnalyticDto>>() {});

            // 5. Повертаємо потрібний день або новий, якщо в мапі його ще немає
            DataAnalyticDto dto = monthData.get(mapKey);
            if (dto != null) {
                log.info("Завантажено існуючий DTO для {}", mapKey);
                return dto;
            }
        } catch (IOException e) {
            log.error("Помилка парсингу JSON аналітики: {}", e.getMessage());
        }
        return new DataAnalyticDto(location, powerType);
    }

}
