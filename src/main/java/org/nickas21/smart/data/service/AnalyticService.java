package org.nickas21.smart.data.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.DefaultSmartSolarmanTuyaService;
import org.nickas21.smart.PowerValueRealTimeData;
import org.nickas21.smart.data.dataEntityDto.DataAnalyticDto;
import org.nickas21.smart.data.dataEntityDto.DataHomeDto;
import org.nickas21.smart.data.dataEntityDto.DataTemperatureDto;
import org.nickas21.smart.tuya.TuyaDeviceService;
import org.nickas21.smart.util.LocationType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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

import static org.nickas21.smart.data.dataEntityDto.DataHomeDto.updateTimeStampToUtc;

@Slf4j
@Service
public class AnalyticService {

    @Value("${smart.analytic.dir:./analytic/}")
    private String dirAnalytic;

    @Value("${smart.analytic.zones.day-start}")
    private int dayStart;

    @Value("${smart.analytic.zones.night-start}")
    private int nightStart;

    @Value("${smart.analytic.golego.update-rate:300000}")
    private long updateRateMs;

    private final Map<String, List<DataAnalyticDto>> analyticCache = new HashMap<>();

    private final DataHomeService dataHomeService;
    private final DefaultSmartSolarmanTuyaService solarmanTuyaService;
    private final TuyaDeviceService tuyaDeviceService;
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

    private void removeFromCache(LocalDate today, LocationType locationType) {
        // Межа: вчорашній день. Все, що було ДО вчора (тобто позавчора і раніше), видаляємо.
        LocalDate beforeYesterday = today.minusDays(2);
        this.analyticCache.keySet().removeIf(key -> {
            try {
                // Витягуємо дату з ключа "2026-03-12_DACHA"
                String datePart = key.split("_")[0];
                LocalDate entryDate = LocalDate.parse(datePart);
                // Якщо дата запису РАНІШЕ за вчора (це і є -2, -3 і т.д.)
                if (entryDate.isBefore(beforeYesterday)) {
                    List<DataAnalyticDto> oldData = this.analyticCache.get(key);
                    if (oldData != null && !oldData.isEmpty()) {
                        // Зберігаємо в файл перед видаленням
                        saveToMonthlyFile(entryDate, oldData, locationType);
                    }
                    return true; // Видаляємо з кешу
                }
            } catch (Exception e) {
                // Пропускаємо, якщо формат ключа не той
            }
            return false;
        });
    }

    public AnalyticService(DataHomeService dataHomeService, DefaultSmartSolarmanTuyaService solarmanTuyaService, TuyaDeviceService tuyaDeviceService) {
        this.dataHomeService = dataHomeService;
        this.solarmanTuyaService = solarmanTuyaService;
        this.tuyaDeviceService = tuyaDeviceService;
    }

    @Scheduled(fixedRateString = "${smart.analytic.update-rate:300000}")
    public void updateAndSaveAnalytic() {
        for (LocationType locationType : LocationType.values()) {

            LocalDate today = getLocalDateInverter(locationType);
            LocalDate yesterday = today.minusDays(1);
            String todayKey = generateMapDateKey(today, locationType);
            String yesterdayKey = generateMapDateKey(yesterday, locationType);
            synchronized (this.analyticCache) {
                // ПЕРЕВІРКА ПЕРЕХОДУ:
                // Вчорашні дані ще в кеші, а сьогоднішніх — ще не було.
                if (this.analyticCache.containsKey(yesterdayKey) && !this.analyticCache.containsKey(todayKey)) {
                    log.info("Зафіксовано перехід доби для {}. Очищаємо кеш від застарілих даних.", locationType);
                    removeFromCache(today, locationType);
                }
                // Оновлюємо поточні дані
                switch (locationType) {
                    case DACHA -> updateAnalyticDacha();
                    case GOLEGO -> updateAnalyticGolego();
                }
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
            Map<String, List<DataAnalyticDto>> allMonthData = objectMapper.readValue(json,
                    new TypeReference<LinkedHashMap<String, List<DataAnalyticDto>>>() {});

            return allMonthData.values().stream()
                    .filter(dayList -> !dayList.isEmpty())
                    // БЕРЕМО ОСТАННЮ ТОЧКУ ДНЯ (це і є Daily за день)
                    .map(List::getLast)
                    .sorted(Comparator.comparingLong(DataAnalyticDto::getTimestamp))
                    .toList();
        } catch (IOException e) {
            log.error("Помилка: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<DataAnalyticDto> getAnalyticForYear(int year, LocationType location) {
        List<DataAnalyticDto> yearData = new ArrayList<>();

        // Проходимо по всіх 12 місяцях (01, 02 ... 12)
        for (int month = 1; month <= 12; month++) {
            String monthSuffix = String.format("%d-%02d", year, month);
            Path path = getPathFile(location, monthSuffix);

            if (Files.exists(path)) {
                try {
                    String json = Files.readString(path, StandardCharsets.UTF_8);
                    Map<String, List<DataAnalyticDto>> allMonthData = objectMapper.readValue(json,
                            new TypeReference<LinkedHashMap<String, List<DataAnalyticDto>>>() {});

                    // Агрегуємо всі дні цього місяця в одну "точку місяця"
                    DataAnalyticDto monthSummary = aggregateMonth(allMonthData, monthSuffix);
                    yearData.add(monthSummary);

                } catch (IOException e) {
                    log.error("Помилка парсингу за місяць {}: {}", monthSuffix, e.getMessage());
                }
            }
        }
        return yearData;
    }

    private DataAnalyticDto aggregateMonth(Map<String, List<DataAnalyticDto>> allMonthData, String monthSuffix) {
        DataAnalyticDto summary = new DataAnalyticDto();
        // Встановлюємо таймстамп на початок місяця для сортування на фронті
        summary.setTimestamp(parseMonthToTimestamp(monthSuffix));

        double yearSolarDailyPower = 0;
        double yearHomeDailyPower = 0;
        double yearGridDailyDayPower = 0;
        double yearDailyNightPower = 0;
        double yearGridDailyTotalPower = 0;
        double yearDailyDischarge = 0;
        double yearDailyCharge = 0;

        for (List<DataAnalyticDto> dayPoints : allMonthData.values()) {
            if (!dayPoints.isEmpty()) {
                // Беремо останню точку дня (фінальне накопичене значення)
                DataAnalyticDto lastPointOfDay = dayPoints.getLast();

                yearSolarDailyPower += lastPointOfDay.getSolarDailyPower();
                yearHomeDailyPower += lastPointOfDay.getHomeDailyPower();
                yearGridDailyDayPower += lastPointOfDay.getGridDailyDayPower();
                yearDailyNightPower += lastPointOfDay.getGridDailyNightPower();
                yearGridDailyTotalPower += lastPointOfDay.getGridDailyTotalPower();
                yearDailyDischarge += lastPointOfDay.getBmsDailyDischarge();
                yearDailyCharge += lastPointOfDay.getBmsDailyCharge();
            }
        }

        // Записуємо суми в поля Daily (фронт їх підхопить як значення стовпчиків)
        summary.setSolarDailyPower(yearSolarDailyPower);
        summary.setHomeDailyPower(yearHomeDailyPower);
        summary.setGridDailyDayPower(yearGridDailyDayPower);
        summary.setGridDailyNightPower(yearDailyNightPower);
        summary.setGridDailyTotalPower(yearGridDailyTotalPower);
        summary.setBmsDailyDischarge(yearDailyDischarge);
        summary.setBmsDailyCharge(yearDailyCharge);
        return summary;
    }

    private long parseMonthToTimestamp(String monthSuffix) {
        try {
            // monthSuffix приходить як "2026-03"
            java.time.YearMonth yearMonth = java.time.YearMonth.parse(monthSuffix);

            // Перетворюємо в 1-ше число місяця, 00:00:00 UTC
            return yearMonth.atDay(1)
                    .atStartOfDay(java.time.ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli();
        } catch (Exception e) {
            log.error("Помилка парсингу суфікса місяця {}: {}", monthSuffix, e.getMessage());
            return 0L;
        }
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
            log.error("Failed to save file for {}: ", monthSuffix, e);
            throw new RuntimeException("FileSystem error during import for " + monthSuffix + ": " + e.getMessage());
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
        PowerValueRealTimeData data = solarmanTuyaService.getPowerValueRealTimeData();
        long ts = data.getCollectionTime() * 1000;
        if (ts < 1000000000000L) return;

        LocationType locationType = LocationType.DACHA;
        ZonedDateTime zdt = getZonedDateTimeInverter(ts, locationType);
        String key = generateMapDateKey(zdt.toLocalDate(), locationType);

        List<DataAnalyticDto> dayList = this.analyticCache.computeIfAbsent(key,
                k -> Collections.synchronizedList(new ArrayList<>()));
        long offsetMs = updateTimeStampToUtc(ts, locationType.getZoneId());
        long finalTs = ts + offsetMs;
        if (dayList.stream().anyMatch(e -> e.getTimestamp() == finalTs)) return;

        DataAnalyticDto dtoDacha = new DataAnalyticDto(locationType);
        dtoDacha.setTimestamp(finalTs); // ЧИСТИЙ ЧАС
        dtoDacha.setGridDailyTotalPower(data.getDailyEnergyBuy());
        dtoDacha.setBmsSoc(data.getBatterySocValue());
        dtoDacha.setSolarDailyPower(data.getDailyProductionSolarPower());
        dtoDacha.setHomeDailyPower(data.getDailyHomeConsumptionPower());
        dtoDacha.setGridPower(data.getTotalGridPower());
        dtoDacha.setSolarPower(data.getTotalProductionSolarPower());
        dtoDacha.setHomePower(data.getTotalHomePower());
        dtoDacha.setBmsDailyDischarge(data.getDailyBatteryDischarge());
        dtoDacha.setBmsDailyCharge(data.getDailyBatteryCharge());
        DataTemperatureDto temperatureDto = tuyaDeviceService.getTemperatureValueById(tuyaDeviceService.deviceIdTemperatureOutDacha);
        if (temperatureDto != null) {
            dtoDacha.setTemperatureOut(temperatureDto.getTemperature());
            dtoDacha.setHumidityOut(temperatureDto.getHumidity());
            dtoDacha.setLuminanceOut(temperatureDto.getLuminance());
        }
        temperatureDto = tuyaDeviceService.getTemperatureValueById(tuyaDeviceService.deviceIdTemperatureInDacha);
        if (temperatureDto != null) {
            dtoDacha.setTemperatureIn(temperatureDto.getTemperature());
            dtoDacha.setHumidityIn(temperatureDto.getHumidity());
            dtoDacha.setLuminanceIn(temperatureDto.getLuminance());
        }
        // ВИКЛИК СПІЛЬНОЇ ЛОГІКИ
        DataAnalyticDto last = dayList.isEmpty() ? null : dayList.getLast();
        calculateGridTariffs(dtoDacha, last);

        dayList.add(dtoDacha);
        dayList.sort(Comparator.comparingLong(DataAnalyticDto::getTimestamp));
        saveToMonthlyFile(finalTs, locationType);
    }

    private synchronized void updateAnalyticGolego() {
        DataHomeDto dataHomeDto = dataHomeService.getDataGolego();
        LocationType locationType = LocationType.GOLEGO;
        long timestampRaw = dataHomeDto.getTimestamp();
        if (timestampRaw < 1000000000000L) { // Перевірка, що дата не з 1970-х років
            log.warn("Invalid timestamp GOLEGO) [{}]", timestampRaw);
            return;
        }
        ZonedDateTime zonedDateTimeInverter = getZonedDateTimeInverter(timestampRaw, locationType);
        String mapDateKey = generateMapDateKey(zonedDateTimeInverter.toLocalDate(), locationType);

        List<DataAnalyticDto> dayList = this.analyticCache.computeIfAbsent(mapDateKey,
                k -> Collections.synchronizedList(new ArrayList<>()));

        // 1. ПЕРЕВІРКА НА ДУБЛІКАТ
        long finalTs  = timestampRaw + (zonedDateTimeInverter.getOffset().getTotalSeconds() * 1000L);
        if (dayList.stream().anyMatch(e -> e.getTimestamp() == finalTs)) return;

        // 2. СОРТУВАННЯ перед розрахунками
        dayList.sort(Comparator.comparingLong(DataAnalyticDto::getTimestamp));

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

        if (!dayList.isEmpty()) {
            DataAnalyticDto last = dayList.getLast();
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

        DataAnalyticDto dtoGolego = new DataAnalyticDto(locationType);
        dtoGolego.setTimestamp(finalTs);
        dtoGolego.setGridPower(currentGridPower);
        dtoGolego.setSolarPower(0);
        dtoGolego.setHomePower(dataHomeDto.getHomePower());
        dtoGolego.setBmsSoc(dataHomeDto.getBatterySoc());

        dtoGolego.setGridDailyDayPower(dailyGridGolegoDay);
        dtoGolego.setGridDailyNightPower(dailyGridGolegoNight);
        dtoGolego.setGridDailyTotalPower(dailyGridGolegoDay + dailyGridGolegoNight);

        // ПРАВИЛЬНО: додаємо кВт·год споживання дому
        dtoGolego.setHomeDailyPower(dailyHomePower + deltaHomeKwh);

        // Розрахунок заряду/розряду BMS
        if (currentBmsPowerKwh < 0) {
            dtoGolego.setBmsDailyDischarge(dailyBmsDischarge + Math.abs(currentBmsPowerKwh));
            dtoGolego.setBmsDailyCharge(dailyBmsCharge);
        } else {
            dtoGolego.setBmsDailyCharge(dailyBmsCharge + currentBmsPowerKwh);
            dtoGolego.setBmsDailyDischarge(dailyBmsDischarge); // Це добре
        }
        dayList.add(dtoGolego);
        dayList.sort(Comparator.comparingLong(DataAnalyticDto::getTimestamp));
        saveToMonthlyFile(finalTs, locationType);
    }

    private synchronized void saveToMonthlyFile(long timestamp, LocationType location) {
        ZonedDateTime zdt = getZonedDateTimeInverter(timestamp, location);
        String monthSuffix = zdt.format(DateTimeFormatter.ofPattern(patternMonthFile));
        Path path = getPathFile(location, monthSuffix);

        try {
            // 1. Створюємо директорії лише якщо їх реально немає
            Path parent = path.getParent();
            if (parent != null && Files.notExists(parent)) {
                Files.createDirectories(parent);
            }

            // 2. Використовуємо LinkedHashMap для збереження порядку днів у файлі
            Map<String, List<DataAnalyticDto>> monthData = new LinkedHashMap<>();

            // 3. Читаємо файл через потік (Stream), це швидше і споживає менше пам'яті, ніж readString
            if (Files.exists(path)) {
                try (InputStream is = Files.newInputStream(path)) {
                    monthData = objectMapper.readValue(is, new TypeReference<LinkedHashMap<String, List<DataAnalyticDto>>>() {});
                }
            }

            // 4. Оновлюємо дані лише для конкретного дня
            String mapDateKey = generateMapDateKey(zdt.toLocalDate(), location);
            List<DataAnalyticDto> dailyCache = this.analyticCache.get(mapDateKey);

            if (dailyCache != null) {
                // Робимо копію списку, щоб уникнути ConcurrentModificationException, якщо кеш оновиться під час серіалізації
                monthData.put(mapDateKey, new ArrayList<>(dailyCache));

                // 5. Записуємо відразу в файл через InputStream/OutputStream — це ефективніше для великих JSON
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), monthData);
            }

        } catch (IOException e) {
            log.error("Failed to save file for {}: {}", monthSuffix, e.getMessage());
            throw new RuntimeException("FileSystem error for " + monthSuffix, e);
        }
    }

    public synchronized List<DataAnalyticDto> importXmlsData(String rawJson) {
        List<DataAnalyticDto> incomingPoints;
        try {
            List<DataAnalyticDto> incomingLocalPoints = objectMapper.readValue(rawJson, new TypeReference<>(){});
            if (incomingLocalPoints == null || incomingLocalPoints.isEmpty()) {
                log.error("Import aborted: incomingLocalPoints is empty");
                return new ArrayList<>();
            }

            incomingPoints = updateTimeStampToUtc(incomingLocalPoints);
            Map<String, List<DataAnalyticDto>> pointsByMonth = incomingPoints.stream()
                    .collect(Collectors.groupingBy(p ->
                            Instant.ofEpochMilli(p.getTimestamp())
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate()
                                    .format(DateTimeFormatter.ofPattern(patternMonthFile))
                    ));
            pointsByMonth.forEach((monthSuffix, points) -> {
                LocationType loc = points.getFirst().getLocation();
                Path path = getPathFile(loc, monthSuffix);
                try {
                    // 1. Директорії
                    Path parent = path.getParent();
                    if (parent != null && Files.notExists(parent)) {
                        Files.createDirectories(parent);
                    }

                    // 2. Читання файлу
                    Map<String, List<DataAnalyticDto>> monthData = new LinkedHashMap<>();
                    if (Files.exists(path)) {
                        try (InputStream is = Files.newInputStream(path)) {
                            monthData = objectMapper.readValue(is, new TypeReference<LinkedHashMap<String, List<DataAnalyticDto>>>() {});
                        } catch (Exception e) {
                            log.error("[{}] CRITICAL READ ERROR: {}", monthSuffix, e.getMessage(), e);
                        }
                    } else {
                        log.error("[{}] File does not exist yet. Creating new map.", monthSuffix);
                    }

                    // 3. Обробка точок
                    int count = 0;
                    for (DataAnalyticDto p : points) {
                        ZonedDateTime zdt = Instant.ofEpochMilli(p.getTimestamp()).atZone(ZoneOffset.UTC);
                        String mapDateKey = generateMapDateKey(zdt.toLocalDate(), p.getLocation());

                        List<DataAnalyticDto> dayList = monthData.computeIfAbsent(mapDateKey, k -> new ArrayList<>());
                        dayList.removeIf(old -> old.getTimestamp() == p.getTimestamp());
                        dayList.add(p);

                        count++;
                        if (count % 100 == 0) log.info("[{}] Processed {}/{} points...", monthSuffix, count, points.size());
                    }
                    monthData.values().forEach(list -> {
                        list.sort(Comparator.comparingLong(DataAnalyticDto::getTimestamp));
                        DataAnalyticDto last = null;
                        for (DataAnalyticDto p : list) {
                            calculateGridTariffs(p, last);
                            last = p;
                        }
                    });
                    byte[] jsonBytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(monthData);
                    Files.write(path, jsonBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    monthData.forEach((key, list) -> {
                        this.analyticCache.put(key, Collections.synchronizedList(new ArrayList<>(list)));
                    });
                } catch (Exception e) {
                    log.error("[{}] ERROR IN MONTH BLOCK: {}", monthSuffix, e.getMessage(), e);
                    throw new RuntimeException("Import failed for " + monthSuffix, e);
                }
            });
        } catch (Exception e) {
            log.error("!!! GLOBAL IMPORT ERROR !!!: {}", e.getMessage(), e);
            throw new RuntimeException("FileSystem error for ", e);
        }
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

    private void calculateGridTariffs(DataAnalyticDto current, DataAnalyticDto last) {
        double totalGrid = current.getGridDailyTotalPower();

        // Якщо це перша точка за добу
        if (last == null) {
            ZonedDateTime zdt = Instant.ofEpochMilli(current.getTimestamp()).atZone(current.getLocation().getZoneId());
            int hour = zdt.getHour();
            if (hour < dayStart || hour >= nightStart) {
                current.setGridDailyNightPower(totalGrid);
                current.setGridDailyDayPower(0.0);
            } else {
                current.setGridDailyDayPower(totalGrid);
                current.setGridDailyNightPower(0.0);
            }
            return;
        }
        double delta = totalGrid - last.getGridDailyTotalPower();
        if (delta < 0) delta = 0;

        ZonedDateTime zdt = Instant.ofEpochMilli(current.getTimestamp()).atZone(current.getLocation().getZoneId());
        int hour = zdt.getHour();

        if (hour < dayStart || hour >= nightStart) {
            current.setGridDailyNightPower(last.getGridDailyNightPower() + delta);
            current.setGridDailyDayPower(last.getGridDailyDayPower());
        } else {
            current.setGridDailyDayPower(last.getGridDailyDayPower() + delta);
            current.setGridDailyNightPower(last.getGridDailyNightPower());
        }
    }
}