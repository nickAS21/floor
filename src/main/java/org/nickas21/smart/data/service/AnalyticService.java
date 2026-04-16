package org.nickas21.smart.data.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.DefaultSmartSolarmanTuyaService;
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

import static java.time.ZoneOffset.UTC;
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
                updateAnalytic(locationType);
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
                    .atStartOfDay(UTC)
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

    private synchronized void updateAnalytic(LocationType locationType) {
        // 1. Отримуємо DTO залежно від локації
        DataHomeDto dataHomeDto = (locationType == LocationType.DACHA)
                ? dataHomeService.getDataDacha()
                : dataHomeService.getDataGolego();

        long finalTs = dataHomeDto.getTimestamp();
        if (finalTs < 1000000000000L) return;

        // 2. Підготовка TZ та ключів
        ZonedDateTime zdtLocal = getZonedDateTimeInverter(finalTs, locationType);
        String mapDateKey = generateMapDateKey(zdtLocal.toLocalDate(), locationType);

        // 3. Робота з кешем
        List<DataAnalyticDto> dayList = this.analyticCache.computeIfAbsent(mapDateKey,
                k -> Collections.synchronizedList(new ArrayList<>()));
        if (dayList.stream().anyMatch(e -> e.getTimestamp() == finalTs)) return;

        DataAnalyticDto last = dayList.isEmpty() ? null : dayList.getLast();
        DataAnalyticDto analyticDto = new DataAnalyticDto(locationType);

        // 4. Спільні поля
        analyticDto.setTimestamp(finalTs);
        analyticDto.setGridPower(dataHomeDto.getGridPower());
        analyticDto.setHomePower(dataHomeDto.getHomePower());
        analyticDto.setBmsSoc(dataHomeDto.getBatterySoc());
        analyticDto.setSolarPower(dataHomeDto.getSolarPower());
        analyticDto.setSolarDailyPower(dataHomeDto.getDailyProductionSolarPower());

        // 5. Специфічна логіка розрахунку Daily (Dacha vs Golego)
        if (locationType == LocationType.DACHA) {
            // Логіка DACHA: Беремо готові лічильники з DTO
            double lastTotal = (last != null) ? last.getGridDailyTotalPower() : 0.0;
            double deltaGrid = Math.max(0, dataHomeDto.getDailyGridPower() - lastTotal);
            applyGridTariffLogic(analyticDto, last, deltaGrid, zdtLocal.getHour());

            analyticDto.setGridDailyTotalPower(dataHomeDto.getDailyGridPower());
            analyticDto.setHomeDailyPower(dataHomeDto.getDailyConsumptionPower());
            analyticDto.setBmsDailyDischarge(dataHomeDto.getDailyBatteryDischarge());
            analyticDto.setBmsDailyCharge(dataHomeDto.getDailyBatteryCharge());

            // Додаємо температуру (тільки для Dacha)
            fillTemperatureData(analyticDto);
        } else {
            // Логіка GOLEGO: Розраховуємо дельти інтегрально
            double deltaTime = updateRateMs / 3600000.0;
            double deltaGridKwh = Math.max(0, (dataHomeDto.getGridPower() / 1000.0) * deltaTime);
            double deltaHomeKwh = (dataHomeDto.getHomePower() / 1000.0) * deltaTime;
            double deltaBmsKwh = (dataHomeDto.getBatteryVol() * dataHomeDto.getBatteryCurrent() * deltaTime) / 1000.0;

            applyGridTariffLogic(analyticDto, last, deltaGridKwh, zdtLocal.getHour());
            analyticDto.setGridDailyTotalPower(analyticDto.getGridDailyDayPower() + analyticDto.getGridDailyNightPower());

            double prevHome = (last != null) ? last.getHomeDailyPower() : 0.0;
            double prevDischarge = (last != null) ? last.getBmsDailyDischarge() : 0.0;
            double prevCharge = (last != null) ? last.getBmsDailyCharge() : 0.0;

            analyticDto.setHomeDailyPower(prevHome + deltaHomeKwh);
            if (deltaBmsKwh < 0) {
                analyticDto.setBmsDailyDischarge(prevDischarge + Math.abs(deltaBmsKwh));
                analyticDto.setBmsDailyCharge(prevCharge);
            } else {
                analyticDto.setBmsDailyCharge(prevCharge + deltaBmsKwh);
                analyticDto.setBmsDailyDischarge(prevDischarge);
            }
        }

        // 6. Фіналізація
        dayList.add(analyticDto);
        dayList.sort(Comparator.comparingLong(DataAnalyticDto::getTimestamp));
        saveToMonthlyFile(finalTs, locationType);
    }

    // Допоміжний метод для температури, щоб не захаращувати основний
    private void fillTemperatureData(DataAnalyticDto dto) {
        DataTemperatureDto tempOut = tuyaDeviceService.getTemperatureValueById(tuyaDeviceService.deviceIdTemperatureOutDacha);
        if (tempOut != null) {
            dto.setTemperatureOut(tempOut.getTemperature());
            dto.setHumidityOut(tempOut.getHumidity());
            dto.setLuminanceOut(tempOut.getLuminance());
        }
        DataTemperatureDto tempIn = tuyaDeviceService.getTemperatureValueById(tuyaDeviceService.deviceIdTemperatureInDacha);
        if (tempIn != null) {
            dto.setTemperatureIn(tempIn.getTemperature());
            dto.setHumidityIn(tempIn.getHumidity());
            dto.setLuminanceIn(tempIn.getLuminance());
        }
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
                                    .atZone(UTC)
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
                        ZonedDateTime zdt = Instant.ofEpochMilli(p.getTimestamp()).atZone(UTC);
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
        return date.toString() + separatorKey + location.name();
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

    private void applyGridTariffLogic(DataAnalyticDto current, DataAnalyticDto last, double deltaGrid, int hour) {
        // Беремо значення з попередньої точки (якщо вона є)
        double prevDay = (last != null) ? last.getGridDailyDayPower() : 0.0;
        double prevNight = (last != null) ? last.getGridDailyNightPower() : 0.0;

        // Додаємо дельту у відповідний тариф
        if (hour < dayStart || hour >= nightStart) {
            current.setGridDailyNightPower(prevNight + deltaGrid);
            current.setGridDailyDayPower(prevDay);
        } else {
            current.setGridDailyDayPower(prevDay + deltaGrid);
            current.setGridDailyNightPower(prevNight);
        }
    }
}