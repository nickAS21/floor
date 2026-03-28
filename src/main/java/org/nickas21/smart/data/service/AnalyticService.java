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
import org.nickas21.smart.data.dataEntityDto.DataTemperatureDto;
import org.nickas21.smart.tuya.TuyaDeviceService;
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
                    case GOLEGO -> updateGolegoAnalytic();
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
                    .map(dayList -> dayList.get(dayList.size() - 1))
                    .sorted(Comparator.comparingLong(DataAnalyticDto::getTimestamp))
                    .toList();
        } catch (IOException e) {
            log.error("Помилка: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
//
//    public List<DataAnalyticDto> getAnalyticForMonth(LocationType location, String monthSuffix) {
//        Path path = getPathFile(location, monthSuffix);
//        if (!Files.exists(path)) return new ArrayList<>();
//        try {
//            String json = Files.readString(path, StandardCharsets.UTF_8);
//            Map<String, List<DataAnalyticDto>> allMonthData = objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, List<DataAnalyticDto>>>() {});
//            return allMonthData.values().stream().flatMap(List::stream).sorted(Comparator.comparingLong(DataAnalyticDto::getTimestamp)).toList();
//        } catch (IOException e) {
//            log.error("Помилка історії за місяць: {}", e.getMessage());
//            return new ArrayList<>();
//        }
//    }

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
                DataAnalyticDto lastPointOfDay = dayPoints.get(dayPoints.size() - 1);

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
        PowerValueRealTimeData data = solarmanTuyaService.getPowerValueRealTimeData();
        long ts = data.getCollectionTime() * 1000;
        if (ts < 1000000000000L) return;

        LocationType loc = LocationType.DACHA;
        ZonedDateTime zdt = getZonedDateTimeInverter(ts, loc);
        String key = generateMapDateKey(zdt.toLocalDate(), loc);

        List<DataAnalyticDto> dayList = this.analyticCache.computeIfAbsent(key,
                k -> Collections.synchronizedList(new ArrayList<>()));
        long offsetMs = updateTimeStampToUtc(ts, LocationType.DACHA.getZoneId());
        long finalTs = ts + offsetMs;
        if (dayList.stream().anyMatch(e -> e.getTimestamp() == finalTs)) return;

        DataAnalyticDto dto = new DataAnalyticDto(loc);
        dto.setTimestamp(finalTs); // ЧИСТИЙ ЧАС
        dto.setGridDailyTotalPower(data.getDailyEnergyBuy());
        dto.setBmsSoc(data.getBatterySocValue());
        dto.setSolarDailyPower(data.getDailyProductionSolarPower());
        dto.setHomeDailyPower(data.getDailyHomeConsumptionPower());
        dto.setGridPower(data.getTotalGridPower());
        dto.setSolarPower(data.getTotalProductionSolarPower());
        dto.setHomePower(data.getTotalHomePower());
        dto.setBmsDailyDischarge(data.getDailyBatteryDischarge());
        dto.setBmsDailyCharge(data.getDailyBatteryCharge());
        DataTemperatureDto temperatureDto = tuyaDeviceService.getTemperatureValueById(tuyaDeviceService.deviceIdTemperatureOutDacha);
        if (temperatureDto != null) {
            dto.setTemperatureOut(temperatureDto.getTemperature());
            dto.setHumidityOut(temperatureDto.getHumidity());
            dto.setLuminanceOut(temperatureDto.getLuminance());
        }
        temperatureDto = tuyaDeviceService.getTemperatureValueById(tuyaDeviceService.deviceIdTemperatureInDacha);
        if (temperatureDto != null) {
            dto.setTemperatureIn(temperatureDto.getTemperature());
            dto.setHumidityIn(temperatureDto.getHumidity());
            dto.setLuminanceIn(temperatureDto.getLuminance());
        }
        // ВИКЛИК СПІЛЬНОЇ ЛОГІКИ
        DataAnalyticDto last = dayList.isEmpty() ? null : dayList.getLast();
        calculateGridTariffs(dto, last);

        dayList.add(dto);
        dayList.sort(Comparator.comparingLong(DataAnalyticDto::getTimestamp));
        saveToMonthlyFile(dto);
    }

    private synchronized void updateGolegoAnalytic() {
        DataHomeDto dataHomeDto = dataHomeService.getDataGolego();
        LocationType locationType = LocationType.GOLEGO;
        long timestampRaw = dataHomeDto.getTimestamp();
        if (timestampRaw < 1000000000000L) { // Перевірка, що дата не з 1970-х років
            log.warn("Invalid timestamp GOLEGO) [{}]", timestampRaw);
            return;
        }
        ZonedDateTime zonedDateTimeInverter = getZonedDateTimeInverter(timestampRaw, locationType);
        String mapDateKey = generateMapDateKey(zonedDateTimeInverter.toLocalDate(), locationType);

        List<DataAnalyticDto> timeDateAnalyticDtos = this.analyticCache.computeIfAbsent(mapDateKey,
                k -> Collections.synchronizedList(new ArrayList<>()));

        // 1. ПЕРЕВІРКА НА ДУБЛІКАТ
        long timestamp  = timestampRaw + (zonedDateTimeInverter.getOffset().getTotalSeconds() * 1000L);
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

    public synchronized List<DataAnalyticDto> importXmlsData(List<DataAnalyticDto> incomingLocalPoints) {
        if (incomingLocalPoints == null || incomingLocalPoints.isEmpty()) return new ArrayList<>();
        List<DataAnalyticDto> incomingPoints =  updateTimeStampToUtc(incomingLocalPoints);
        Map<String, List<DataAnalyticDto>> pointsByMonth = incomingPoints.stream()
                .collect(Collectors.groupingBy(p ->
                        Instant.ofEpochMilli(p.getTimestamp())
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                                .format(DateTimeFormatter.ofPattern(patternMonthFile))
                ));
        pointsByMonth.forEach((monthSuffix, points) -> {
            DataAnalyticDto first = points.getFirst();
            Path path = getPathFile(first.getLocation(), monthSuffix);
            try {
                Map<String, List<DataAnalyticDto>> monthData = new LinkedHashMap<>();
                if (Files.exists(path)) {
                    String json = Files.readString(path, StandardCharsets.UTF_8);
                    monthData = objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, List<DataAnalyticDto>>>() {});
                }
                for (DataAnalyticDto p : points) {
//                    ZonedDateTime zdt = Instant.ofEpochMilli(p.getTimestamp()).atZone(p.getLocation().getZoneId());
                    Instant.ofEpochMilli(p.getTimestamp()).atOffset(ZoneOffset.UTC);
                    ZonedDateTime zdt = Instant.ofEpochMilli(p.getTimestamp()).atZone(ZoneOffset.UTC);
                    String mapDateKey = generateMapDateKey(zdt.toLocalDate(), p.getLocation());

                    // Витягуємо список точок за цей день з вашого monthData
                    List<DataAnalyticDto> dayList = monthData.computeIfAbsent(mapDateKey, k -> new ArrayList<>());

                    // Оскільки calculateGridTariffs працює з DTO, можна або зробити такий же для Entity,
                    // або просто вставити логіку прямо сюди.
                    DataAnalyticDto last = dayList.isEmpty() ? null : dayList.getLast();

                    // Розрахунок тарифів для імпортованої точки
                    calculateGridTariffs(p, last);

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
                            .map(e -> new DataAnalyticDto(
                                    e.getTimestamp(),
                                    e.getLocation(),
                                    e.getGridDailyDayPower(),
                                    e.getGridDailyNightPower(),
                                    e.getGridDailyTotalPower()))
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

        // Якщо є попередня точка - рахуємо дельту
        double delta = totalGrid - last.getGridDailyTotalPower();
        if (delta < 0) delta = 0; // Захист від скидання лічильника

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