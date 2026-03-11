package org.nickas21.smart.data.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.DefaultSmartSolarmanTuyaService;
import org.nickas21.smart.PowerValueRealTimeData;
import org.nickas21.smart.data.dataEntityDto.DataAnalytic;
import org.nickas21.smart.data.dataEntityDto.DataAnalyticDto;
import org.nickas21.smart.data.dataEntityDto.DataHomeDto;
import org.nickas21.smart.tuya.TuyaDeviceService;
import org.nickas21.smart.usr.service.UsrTcpWiFiParseData;
import org.nickas21.smart.util.LocationType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AnalyticService {

    @Value("${smart.analytic.dir:./usrAnalytic/}")
    private String dirAnalytic;

    @Value("${smart.analytic.zones.day-start}")
    private int dayStart;

    @Value("${smart.analytic.zones.night-start}")
    private int nightStart;

    @Value("${smart.analytic.golego.update-rate:300000}")
    private long updateRateMs;

    private final Map<String, List<DataAnalyticDto>> analyticCache = new HashMap<>();

    private final DataHomeService dataHomeService;
    private final UsrTcpWiFiParseData usrTcpWiFiParseData;
    private final TuyaDeviceService deviceService;
    private final DefaultSmartSolarmanTuyaService solarmanTuyaService;
    public static final String patternYearFile = "yyyy";
    public static final String patternMonthFile = "yyyy-MM";
    public static final String patternDayKey = "yyyy-MM-dd";
    private static final String separatorKey = "_";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        for (LocationType locationType : LocationType.values()) {
            LocalDate today = getLocalDateInverter(locationType);
            initAndUpdateCash(today, locationType);
        }
    }

    private void initAndUpdateCash(LocalDate today, LocationType locationType) {
        loadAndCache(today, locationType);
        LocalDate yesterday = today.minusDays(1);
        loadAndCache(yesterday, locationType);
    }

    private void loadAndCache(LocalDate date, LocationType locationType) {
        List<DataAnalyticDto> dtos = loadDtosForDate(date, locationType);
        List<DataAnalyticDto> synchronizedList = Collections.synchronizedList(
                dtos != null ? new ArrayList<>(dtos) : new ArrayList<>()
        );
        this.analyticCache.put(generateMapDateKey(date, locationType), synchronizedList);
    }

    private void removeFromCache(LocalDate localDate, LocationType locationType) {
        String mapDateKey = generateMapDateKey(localDate, locationType);
        List<DataAnalyticDto> cachedData = this.analyticCache.remove(mapDateKey);
        if (cachedData != null && !cachedData.isEmpty()) {
            saveToMonthlyFile(localDate, cachedData, locationType);
        }
    }

    public AnalyticService(UsrTcpWiFiParseData usrTcpWiFiParseData, TuyaDeviceService deviceService, DataHomeService dataHomeService, DefaultSmartSolarmanTuyaService solarmanTuyaService) {
        this.usrTcpWiFiParseData = usrTcpWiFiParseData;
        this.deviceService = deviceService;
        this.dataHomeService = dataHomeService;
        this.solarmanTuyaService = solarmanTuyaService;
    }

    @Scheduled(fixedRateString = "${smart.analytic.golego.update-rate:300000}")
    public void updateAndSaveAnalytic() {
        for (LocationType locationType : LocationType.values()) {
            LocalDate today = getLocalDateInverter(locationType);
            LocalDate dayBeforeYesterday = today.minusDays(2);
            removeFromCache(dayBeforeYesterday, locationType);
            switch (locationType) {
                case DACHA -> updateAnalyticDacha();
                case GOLEGO -> updateGolegoAnalytic();
            }
        }
    }

    public List<DataAnalyticDto> getAnalyticByDay(LocalDate date, LocationType locationType) {
        return loadDtosForDate(date, locationType);
    }

    public List<DataAnalyticDto> loadDtosForDate(LocalDate date, LocationType location) {
        String monthSuffix = date.format(DateTimeFormatter.ofPattern(patternMonthFile));
        String mapDateKey = generateMapDateKey(date, location);
        Path path = getPathFile(location, monthSuffix);

        if (this.analyticCache.containsKey(mapDateKey)) {
            return this.analyticCache.get(mapDateKey);
        }

        if (Files.exists(path)) {
            try {
                String content = Files.readString(path, StandardCharsets.UTF_8);
                Map<String, List<DataAnalyticDto>> monthlyMap = objectMapper.readValue(content, new TypeReference<>() {});
                if (monthlyMap != null && monthlyMap.containsKey(mapDateKey)) {
                    List<DataAnalyticDto> dayPoints = monthlyMap.get(mapDateKey);
                    this.analyticCache.put(mapDateKey, dayPoints);
                    return dayPoints;
                }
            } catch (IOException e) {
                log.error("Помилка читання архіву аналітики: {}", e.getMessage());
            }
        }
        return new ArrayList<>();
    }

    public List<DataAnalyticDto> loadDtosForDates(LocalDate dateStart, LocalDate dateFinish, LocationType location) {
        List<DataAnalyticDto> result = new ArrayList<>();
        LocalDate current = dateStart;
        Map<String, Map<String, List<DataAnalyticDto>>> monthlyFilesCache = new HashMap<>();

        while (!current.isAfter(dateFinish)) {
            String monthSuffix = current.format(DateTimeFormatter.ofPattern(patternMonthFile));
            String mapDateKey = generateMapDateKey(current, location);

            if (analyticCache.containsKey(mapDateKey)) {
                result.addAll(analyticCache.get(mapDateKey));
            } else {
                Map<String, List<DataAnalyticDto>> monthlyMap = monthlyFilesCache.computeIfAbsent(monthSuffix, k -> {
                    Path path = getPathFile(location, monthSuffix);
                    if (Files.exists(path)) {
                        try {
                            String content = Files.readString(path, StandardCharsets.UTF_8);
                            return objectMapper.readValue(content, new TypeReference<LinkedHashMap<String, List<DataAnalyticDto>>>() {});
                        } catch (IOException e) {
                            log.error("Error reading range file: {}", e.getMessage());
                        }
                    }
                    return new HashMap<>();
                });
                if (monthlyMap.containsKey(mapDateKey)) {
                    result.addAll(monthlyMap.get(mapDateKey));
                }
            }
            current = current.plusDays(1);
        }
        return result.stream().sorted(Comparator.comparingLong(DataAnalyticDto::getTimestamp)).collect(Collectors.toList());
    }

    public List<DataAnalyticDto> getAnalyticForMonth(LocationType location, String monthSuffix) {
        Path path = getPathFile(location, monthSuffix);
        if (!Files.exists(path)) return new ArrayList<>();
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            Map<String, List<DataAnalyticDto>> allMonthData = objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, List<DataAnalyticDto>>>() {});
            return allMonthData.values().stream().flatMap(List::stream).sorted(Comparator.comparingLong(DataAnalyticDto::getTimestamp)).toList();
        } catch (IOException e) {
            log.error("Помилка історії за місяць: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<DataAnalyticDto> getAnalyticForYear(int year, LocationType location) {
        List<DataAnalyticDto> yearlyData = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            String monthSuffix = String.format("%d-%02d", year, month);
            List<DataAnalyticDto> monthDays = getAnalyticForMonth(location, monthSuffix);
            if (!monthDays.isEmpty()) {
                DataAnalyticDto monthSummary = new DataAnalyticDto(location);
                monthSummary.setTimestamp(monthDays.getFirst().getTimestamp());
                double totalDay = monthDays.stream().mapToDouble(DataAnalyticDto::getGridDailyDayPower).sum();
                double totalNight = monthDays.stream().mapToDouble(DataAnalyticDto::getGridDailyNightPower).sum();
                monthSummary.setGridDailyDayPower(totalDay);
                monthSummary.setGridDailyNightPower(totalNight);
                monthSummary.setGridDailyTotalPower(totalDay + totalNight);
                yearlyData.add(monthSummary);
            }
        }
        return yearlyData;
    }

    private synchronized void saveToMonthlyFile(DataAnalyticDto dto) {
        ZonedDateTime zdt = getZonedDateTimeInverter(dto.getTimestamp(), dto.getLocation());
        LocalDate pointDate = zdt.toLocalDate();
        String monthSuffix = pointDate.format(DateTimeFormatter.ofPattern(patternMonthFile));
        Path path = getPathFile(dto.getLocation(), monthSuffix);
        try {
            if (Files.notExists(path.getParent())) Files.createDirectories(path.getParent());
            Map<String, List<DataAnalyticDto>> monthData;
            if (Files.exists(path)) {
                String json = Files.readString(path, StandardCharsets.UTF_8);
                monthData = objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, List<DataAnalyticDto>>>() {});
            } else {
                monthData = new LinkedHashMap<>();
            }
            String mapDateKey = generateMapDateKey(pointDate, dto.getLocation());
            List<DataAnalyticDto> currentTimeDateAnalyticDtos = this.analyticCache.computeIfAbsent(mapDateKey,
                    k -> Collections.synchronizedList(new ArrayList<>()));
            monthData.put(mapDateKey, new ArrayList<>(currentTimeDateAnalyticDtos));
            String resultJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(monthData);
            Files.writeString(path, resultJson, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.error("Помилка запису: ", e);
        }
    }
    private synchronized void saveToMonthlyFile(LocalDate localDate, List<DataAnalyticDto> dtos, LocationType locationType) {
        String monthSuffix = localDate.format(DateTimeFormatter.ofPattern(patternMonthFile));
        Path path = getPathFile(locationType, monthSuffix);
        try {
            if (Files.notExists(path.getParent())) Files.createDirectories(path.getParent());
            Map<String, List<DataAnalyticDto>> monthData;
            if (Files.exists(path)) {
                String json = Files.readString(path, StandardCharsets.UTF_8);
                monthData = objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, List<DataAnalyticDto>>>() {});
            } else {
                monthData = new LinkedHashMap<>();
            }
            String mapDateKey = generateMapDateKey(localDate, locationType);
            monthData.put(mapDateKey, new ArrayList<>(dtos));
            String resultJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(monthData);
            Files.writeString(path, resultJson, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.error("Помилка запису: ", e);
        }
    }

    private synchronized void updateAnalyticDacha() {
        PowerValueRealTimeData powerValueRealTimeData = solarmanTuyaService.getPowerValueRealTimeData();
        long timestamp = powerValueRealTimeData.getCollectionTime() * 1000;
        LocationType locationType = LocationType.DACHA;
        ZonedDateTime zonedDateTimeInverter = getZonedDateTimeInverter(timestamp, locationType);
        String mapDateKey = generateMapDateKey(zonedDateTimeInverter.toLocalDate(), locationType);
        List<DataAnalyticDto> timeDateAnalyticDtos = this.analyticCache.computeIfAbsent(mapDateKey,
                k -> Collections.synchronizedList(new ArrayList<>()));
        // 3. ПЕРЕВІРКА НА ДУБЛІКАТ (вбиваємо "зрень" 23:51)
        boolean isDuplicate = timeDateAnalyticDtos.stream().anyMatch(e -> e.getTimestamp() == timestamp);
        if (isDuplicate) return;
        timeDateAnalyticDtos.sort(Comparator.comparingLong(DataAnalyticDto::getTimestamp));
        double dailyGridPowerCommon = powerValueRealTimeData.getDailyEnergyBuy();
        double dailyGridDachaNight = 0;
        double dailyGridDachaDay = 0;
        if (!timeDateAnalyticDtos.isEmpty()) {
            DataAnalyticDto last = timeDateAnalyticDtos.getLast();
            dailyGridDachaNight = last.getGridDailyNightPower();
            dailyGridDachaDay = last.getGridDailyDayPower();
        }
        int currentHour = zonedDateTimeInverter.getHour();
        if (currentHour < dayStart) {
            dailyGridDachaNight = dailyGridPowerCommon;
        } else if (currentHour >= nightStart) {
            dailyGridDachaNight = dailyGridPowerCommon - dailyGridDachaDay;
        } else {
            dailyGridDachaDay = dailyGridPowerCommon - dailyGridDachaNight;
        }
        DataAnalyticDto currentDtoDacha = new DataAnalyticDto(locationType);
        currentDtoDacha.setTimestamp(timestamp);
        currentDtoDacha.setGridDailyDayPower(dailyGridDachaDay);
        currentDtoDacha.setGridDailyNightPower(dailyGridDachaNight);
        currentDtoDacha.setGridDailyTotalPower(dailyGridPowerCommon);
        currentDtoDacha.setBmsSoc(powerValueRealTimeData.getBatterySocValue());
        currentDtoDacha.setSolarDailyPower(powerValueRealTimeData.getDailyProductionSolarPower());
        currentDtoDacha.setHomeDailyPower(powerValueRealTimeData.getDailyHomeConsumptionPower());
        currentDtoDacha.setGridPower(powerValueRealTimeData.getTotalGridPower());
        currentDtoDacha.setSolarPower(powerValueRealTimeData.getTotalProductionSolarPower());
        currentDtoDacha.setHomePower(powerValueRealTimeData.getTotalHomePower());
        currentDtoDacha.setBmsDailyDischarge(powerValueRealTimeData.getDailyBatteryDischarge());
        currentDtoDacha.setBmsDailyCharge(powerValueRealTimeData.getDailyBatteryCharge());
        timeDateAnalyticDtos.add(currentDtoDacha);
        this.analyticCache.put(mapDateKey, timeDateAnalyticDtos);
    }

    private synchronized void updateGolegoAnalytic() {
        DataHomeDto dataHomeDto = dataHomeService.getDataGolego();
        LocationType locationType = LocationType.GOLEGO;
        long timestamp = dataHomeDto.getTimestamp();

        ZonedDateTime zonedDateTimeInverter = getZonedDateTimeInverter(timestamp, locationType);
        String mapDateKey = generateMapDateKey(zonedDateTimeInverter.toLocalDate(), locationType);

        List<DataAnalyticDto> timeDateAnalyticDtos = this.analyticCache.computeIfAbsent(mapDateKey,
                k -> Collections.synchronizedList(new ArrayList<>()));

        // 1. ПЕРЕВІРКА НА ДУБЛІКАТ
        boolean isDuplicate = timeDateAnalyticDtos.stream().anyMatch(e -> e.getTimestamp() == timestamp);
        if (isDuplicate) return;

        // 2. СОРТУВАННЯ перед розрахунками
        timeDateAnalyticDtos.sort(Comparator.comparingLong(DataAnalyticDto::getTimestamp));

        double deltaTime = updateRateMs / 3600000.0;
        double currentGridPower = dataHomeDto.getGridPower();

        // Розрахунок дельти енергії в кВт·год (ділимо на 1000)
        double deltaGridKwh = (currentGridPower / 1000.0) * deltaTime;
        double deltaHomeKwh = (dataHomeDto.getHomePower() / 1000.0) * deltaTime;
        // Bms Power теж у кВт·год
        double currentBmsPowerKwh = (dataHomeDto.getBatteryVol() * dataHomeDto.getBatteryCurrent() * deltaTime) / 1000.0;

        double dailyGridGolegoNight = 0;
        double dailyGridGolegoDay = 0;
        double dailyHomePower = 0;
        double dailyBmsDischarge = 0;
        double dailyBmsCharge = 0;

        if (!timeDateAnalyticDtos.isEmpty()) {
            DataAnalyticDto last = timeDateAnalyticDtos.getLast();
            dailyGridGolegoNight = last.getGridDailyNightPower();
            dailyGridGolegoDay = last.getGridDailyDayPower();
            dailyHomePower = last.getHomeDailyPower();
            dailyBmsDischarge = last.getBmsDailyDischarge();
            dailyBmsCharge = last.getBmsDailyCharge();
        }

        // 3. Тарифи День/Ніч (Тільки якщо споживання з мережі > 0)
        int currentHour = zonedDateTimeInverter.getHour(); // використовуємо вже отриманий час
        if (deltaGridKwh > 0) {
            if (currentHour < dayStart || currentHour >= nightStart) dailyGridGolegoNight += deltaGridKwh;
            else dailyGridGolegoDay += deltaGridKwh;
        }

        DataAnalyticDto currentDtoGolego = new DataAnalyticDto(locationType);
        currentDtoGolego.setTimestamp(timestamp);
        currentDtoGolego.setGridPower(currentGridPower);
        currentDtoGolego.setSolarPower(0);
        currentDtoGolego.setHomePower(dataHomeDto.getHomePower());
        currentDtoGolego.setBmsSoc(dataHomeDto.getBatterySoc());

        currentDtoGolego.setGridDailyDayPower(dailyGridGolegoDay);
        currentDtoGolego.setGridDailyNightPower(dailyGridGolegoNight);
        currentDtoGolego.setGridDailyTotalPower(dailyGridGolegoDay + dailyGridGolegoNight);

        // ПРАВИЛЬНО: додаємо кВт·год споживання дому
        currentDtoGolego.setHomeDailyPower(dailyHomePower + deltaHomeKwh);

        // Розрахунок заряду/розряду BMS
        if (currentBmsPowerKwh < 0) {
            currentDtoGolego.setBmsDailyDischarge(dailyBmsDischarge + Math.abs(currentBmsPowerKwh));
            currentDtoGolego.setBmsDailyCharge(dailyBmsCharge);
        } else {
            currentDtoGolego.setBmsDailyCharge(dailyBmsCharge + currentBmsPowerKwh);
            currentDtoGolego.setBmsDailyDischarge(dailyBmsDischarge); // Це добре
        }
        timeDateAnalyticDtos.add(currentDtoGolego);
        saveToMonthlyFile(currentDtoGolego); // переконайся, що він не затирає весь файл
        this.analyticCache.put(mapDateKey, timeDateAnalyticDtos);
    }

    public synchronized List<DataAnalytic> importXmlsData(List<DataAnalytic> incomingPoints) {
        if (incomingPoints == null || incomingPoints.isEmpty()) return new ArrayList<>();
        Map<String, List<DataAnalytic>> pointsByMonth = incomingPoints.stream()
                .collect(Collectors.groupingBy(p ->
                        Instant.ofEpochMilli(p.getTimestamp())
                                .atZone(p.getLocation().getZoneId())
                                .toLocalDate()
                                .format(DateTimeFormatter.ofPattern(patternMonthFile))
                ));
        pointsByMonth.forEach((monthSuffix, points) -> {
            DataAnalytic first = points.getFirst();
            Path path = getPathFile(first.getLocation(), monthSuffix);
            try {
                Map<String, List<DataAnalytic>> monthData = new LinkedHashMap<>();
                if (Files.exists(path)) {
                    String json = Files.readString(path, StandardCharsets.UTF_8);
                    monthData = objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, List<DataAnalytic>>>() {});
                }
                for (DataAnalytic p : points) {
                    LocalDate pointDate = Instant.ofEpochMilli(p.getTimestamp())
                            .atZone(p.getLocation().getZoneId())
                            .toLocalDate();
                    String mapDateKey = generateMapDateKey(pointDate, p.getLocation());
                    List<DataAnalytic> dayList = monthData.computeIfAbsent(mapDateKey, k -> new ArrayList<>());
                    dayList.removeIf(old -> old.getTimestamp() == p.getTimestamp());
                    dayList.add(p);
                    dayList.sort(Comparator.comparingLong(DataAnalytic::getTimestamp));
                }
                Files.createDirectories(path.getParent());
                Files.writeString(path, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(monthData), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                LocalDate today = this.getLocalDateInverter(first.getLocation());
                String todayMapDateKey = generateMapDateKey(today, first.getLocation());
                if (monthData.containsKey(todayMapDateKey)) {
                    List<DataAnalyticDto> dtoList = monthData.get(todayMapDateKey).stream()
                            .map(e -> new DataAnalyticDto(e.getTimestamp(), e.getLocation(), e.getGridDailyDayPower(), e.getGridDailyNightPower(), e.getGridDailyTotalPower()))
                            .collect(Collectors.toList());
//                    if (first.getLocation() == LocationType.DACHA) this.currentDayDachas = dtoList;
//                    else this.currentDayGolegos = dtoList;
                }
            } catch (IOException e) { log.error("Error import: ", e); }
        });
        return incomingPoints;
    }

    private String generateMapDateKey(LocalDate date, LocationType location) {
        return date.toString() + separatorKey + location;
    }

    private Path getPathFile(LocationType location, String monthSuffix) {
        return Paths.get(dirAnalytic, location.name() + separatorKey + monthSuffix + ".json");
    }

    private LocalDate getLocalDateInverter(LocationType locationType) {
        return Instant.now()
                .atZone(locationType.getZoneId())
                .toLocalDate();
    }

    private
    ZonedDateTime getZonedDateTimeInverter(long timestamp, LocationType locationType) {
        return Instant.ofEpochMilli(timestamp)
                .atZone(locationType.getZoneId());
    }
}