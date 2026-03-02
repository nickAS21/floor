package org.nickas21.smart.data.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.DefaultSmartSolarmanTuyaService;
import org.nickas21.smart.PowerValueRealTimeData;
import org.nickas21.smart.data.dataEntityDto.DataAnalyticApiDto;
import org.nickas21.smart.data.dataEntityDto.DataAnalyticDto;
import org.nickas21.smart.data.dataEntityDto.PowerType;
import org.nickas21.smart.solarman.SolarmanStationsService;
import org.nickas21.smart.tuya.TuyaDeviceService;
import org.nickas21.smart.usr.entity.InverterData;
import org.nickas21.smart.usr.entity.InvertorGolegoData90;
import org.nickas21.smart.usr.service.UsrTcpWiFiBatteryRegistry;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.nickas21.smart.data.dataEntityDto.DataHomeDto.golegoInverterPowerDefault;

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

    private final UsrTcpWiFiParseData usrTcpWiFiParseData;
    private final TuyaDeviceService deviceService;
    private final DefaultSmartSolarmanTuyaService solarmanTuyaService;

    private List<DataAnalyticDto> currentDayGridGolegos;
    private double dailyGridGolegoNight;
    private double dailyGridGolegoDay;
    private List<DataAnalyticDto> currentDayGridDachas;
    private double dailyGridDachaNight;
    private double dailyGridDachaDay;
    public static final String patternYearFile = "yyyy";
    public static final String patternMonthFile = "yyyy-MM";
    public static final String patternDayKey = "yyyy-MM-dd";
    private static final String separatorKey = "_";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        LocalDate localDate = LocalDate.now();
        this.currentDayGridGolegos = loadDtosForDate(localDate, LocationType.GOLEGO, PowerType.GRID);
        this.currentDayGridDachas = loadDtosForDate(localDate, LocationType.DACHA, PowerType.GRID);
        this.analyticCache.put(generateDateKey(localDate, LocationType.GOLEGO, PowerType.GRID), this.currentDayGridGolegos);
        this.analyticCache.put(generateDateKey(localDate, LocationType.DACHA, PowerType.GRID), this.currentDayGridDachas);
        // 2. Сортуємо список Dachas за часом, щоб бути впевненими, що остання точка — найсвіжіша
        if (!this.currentDayGridDachas.isEmpty()) {
            this.currentDayGridDachas.sort(Comparator.comparingLong(DataAnalyticDto::getTimestamp));

            // Тепер беремо останню точку з повною впевненістю
            DataAnalyticDto lastPointDacha = this.currentDayGridDachas.get(this.currentDayGridDachas.size() - 1);
            this.dailyGridDachaNight = lastPointDacha.getPowerNight();
            this.dailyGridDachaDay = lastPointDacha.getPowerDay();
        } else {
            this.dailyGridDachaNight = 0.0;
            this.dailyGridDachaDay = 0.0;
        }
        // 3. Сортуємо список Golegos за часом, щоб бути впевненими, що остання точка — найсвіжіша
        if (!this.currentDayGridGolegos.isEmpty()) {
            this.currentDayGridGolegos.sort(Comparator.comparingLong(DataAnalyticDto::getTimestamp));

            // Тепер беремо останню точку з повною впевненістю
            DataAnalyticDto lastPointGolego = this.currentDayGridGolegos.get(this.currentDayGridGolegos.size() - 1);
            this.dailyGridGolegoNight = lastPointGolego.getPowerNight();
            this.dailyGridGolegoDay = lastPointGolego.getPowerDay();
        } else {
            this.dailyGridGolegoNight = 0.0;
            this.dailyGridGolegoDay = 0.0;
        }
    }

    public AnalyticService(UsrTcpWiFiParseData usrTcpWiFiParseData, TuyaDeviceService deviceService, SolarmanStationsService solarmanStationsService, DefaultSmartSolarmanTuyaService solarmanTuyaService) {
        this.usrTcpWiFiParseData = usrTcpWiFiParseData;
        this.deviceService = deviceService;
        this.solarmanTuyaService = solarmanTuyaService;
     }

    @Scheduled(fixedRateString = "${smart.analytic.golego.update-rate:300000}")
    public void updateAndSaveAnalytic() {
        checkAndResetDay();
        updateGolegoGridAnalytic();
        updateDachaGridAnalytic();
        // Оновлюємо кеш для поточної дати
        LocalDate date = LocalDate.now();
        analyticCache.put(generateDateKey(date, LocationType.GOLEGO, PowerType.GRID), currentDayGridGolegos); //
        analyticCache.put(generateDateKey(date, LocationType.DACHA, PowerType.GRID), currentDayGridDachas); //
    }

    // (pattern = AnalyticService.patternDayKey) LocalDate date => "yyyy-MM-dd"
    public List<DataAnalyticDto> getAnalyticByDay(LocalDate date, LocationType location, PowerType type) {
        // 1. Спочатку перевіряємо дату на current
        LocalDate today = LocalDate.now();
        if (today.equals(date)) {
            if (location == LocationType.DACHA) {
                return this.currentDayGridDachas;
            } else if (location == LocationType.GOLEGO) {
                return this.currentDayGridGolegos;
            }
        }
        // 2. Якщо запитують за минулу дату — використовуємо вже написаний loadDtoForDate
        return loadDtosForDate(date, location, type);
    }

   public List<DataAnalyticDto> loadDtosForDate(LocalDate date, LocationType location, PowerType powerType) {
        // 1. Формуємо шляхи та ключі
        String monthSuffix = date.format(DateTimeFormatter.ofPattern(patternMonthFile));
        String mapKey = generateDateKey(date, location, powerType);
        Path path = getPathFile(location, powerType, monthSuffix);

        // 2. ПЕРЕВІРКА КЕШУ: Якщо цей день вже в пам'яті — віддаємо миттєво
        if (this.analyticCache.containsKey(mapKey)) {
            return this.analyticCache.get(mapKey);
        }

        // 3. ДИСК: Якщо в кеші немає, перевіряємо чи існує файл за цей місяць
        if (Files.exists(path)) {
            try {
                // Читаємо весь JSON файл місяця
                String content = Files.readString(path, StandardCharsets.UTF_8);

                // Десеріалізуємо у Map<String, List<DataAnalyticDto>>
                Map<String, List<DataAnalyticDto>> monthlyMap = objectMapper.readValue(
                        content,
                        new TypeReference<>() {}
                );

                // Якщо в мапі є дані для нашого ключа (конкретного дня)
                if (monthlyMap != null && monthlyMap.containsKey(mapKey)) {
                    List<DataAnalyticDto> dayPoints = monthlyMap.get(mapKey);
                    // Кладемо в кеш і повертаємо
                    this.analyticCache.put(mapKey, dayPoints);
                    return dayPoints;
                }
            } catch (IOException e) {
                log.error("Помилка читання архіву аналітики з {}: {}", path, e.getMessage());
            }
        }
        // 4. НОВИЙ СПИСОК: Якщо даних немає ніде (новий день/локація/історія відсутня)
        // - але не кладемо в this.analyticCache, бо можливо з фронта прийде оновлення історії
        return new ArrayList<>();
    }

    public List<DataAnalyticDto> loadDtosForDates(LocalDate dateStart, LocalDate dateFinish, LocationType location, PowerType powerType) {
        List<DataAnalyticDto> result = new ArrayList<>();

        // Визначаємо список унікальних місяців, які охоплює період
        // Щоб не відкривати файл "2026-02.json" десять разів для десяти різних днів
        LocalDate current = dateStart;

        // Тимчасовий кеш для поточної сесії запиту, щоб не парсити файли повторно
        Map<String, Map<String, List<DataAnalyticDto>>> monthlyFilesCache = new HashMap<>();

        while (!current.isAfter(dateFinish)) {
            String monthSuffix = current.format(DateTimeFormatter.ofPattern(patternMonthFile)); //
            String dayKey = generateDateKey(current, location, powerType); //

            // 1. Перевіряємо, чи є дані в RAM кеші (сьогоднішні або нещодавно зчитані)
            if (analyticCache.containsKey(dayKey)) {
                result.addAll(analyticCache.get(dayKey));
            } else {
                // 2. Якщо в RAM немає, вантажимо з диска, використовуючи локальний кеш місяців
                Map<String, List<DataAnalyticDto>> monthlyMap = monthlyFilesCache.computeIfAbsent(monthSuffix, k -> {
                    Path path = getPathFile(location, powerType, monthSuffix); //
                    if (Files.exists(path)) {
                        try {
                            String content = Files.readString(path, StandardCharsets.UTF_8); //
                            return objectMapper.readValue(content, new TypeReference<LinkedHashMap<String, List<DataAnalyticDto>>>() {});
                        } catch (IOException e) {
                            log.error("Error reading range file {}: {}", path, e.getMessage());
                        }
                    }
                    return new HashMap<>();
                });

                if (monthlyMap.containsKey(dayKey)) {
                    result.addAll(monthlyMap.get(dayKey));
                }
            }

            current = current.plusDays(1);
        }

        // Завжди повертаємо відсортований список за часом
        return result.stream()
                .sorted(Comparator.comparingLong(DataAnalyticDto::getTimestamp))
                .collect(Collectors.toList());
    }

    public List<DataAnalyticDto> getAnalyticForMonth(LocationType location, PowerType powerType, String monthSuffix) {
        // Формуємо шлях: ./analytic/dacha_grid_2026-02.json
        Path path = getPathFile(location,  powerType, monthSuffix);
        if (!Files.exists(path)) return new ArrayList<>();

        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            // Читаємо мапу списків (день -> похвилинки)
            Map<String, List<DataAnalyticDto>> allMonthData = objectMapper.readValue(json,
                    new TypeReference<LinkedHashMap<String, List<DataAnalyticDto>>>() {});

            // Перетворюємо мапу в плаский список усіх точок за місяць
            return allMonthData.values().stream()
                    .flatMap(List::stream)
                    .sorted(Comparator.comparingLong(DataAnalyticDto::getTimestamp))
                    .toList();
        } catch (IOException e) {
            log.error("Помилка читання історії за місяць {}: {}", monthSuffix, e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<DataAnalyticDto> getAnalyticForYear(int year, LocationType location, PowerType powerType) {
        List<DataAnalyticDto> yearlyData = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {
            // Формуємо рядок типу "2026-02"
            String monthSuffix = String.format("%d-%02d", year, month);

            // Використовуємо вже готовий метод для отримання даних за місяць
            List<DataAnalyticDto> monthDays = getAnalyticForMonth(location, powerType, monthSuffix);

            if (!monthDays.isEmpty()) {
                // Агрегуємо всі дні місяця в один об'єкт для графіка "Рік по місяцях"
                DataAnalyticDto monthSummary = new DataAnalyticDto(location, powerType);
                monthSummary.setTimestamp(monthDays.get(0).getTimestamp()); // Мітка для сортування

                double totalDay = monthDays.stream().mapToDouble(DataAnalyticDto::getPowerDay).sum();
                double totalNight = monthDays.stream().mapToDouble(DataAnalyticDto::getPowerNight).sum();

                monthSummary.setPowerDay(totalDay);
                monthSummary.setPowerNight(totalNight);
                monthSummary.setPowerTotal(totalDay + totalNight);

                yearlyData.add(monthSummary);
            }
        }
        return yearlyData;
    }

    private synchronized void saveToMonthlyFile(DataAnalyticDto dto) {
        LocalDate today = LocalDate.now();
        String monthSuffix = today.format(DateTimeFormatter.ofPattern(patternMonthFile));
        Path path = getPathFile(dto.getLocation(), dto.getPowerType(), monthSuffix);

        try {
            // 1. Створюємо папки, якщо це новий місяць
            if (Files.notExists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }

            // 2. Беремо дані з диска (якщо файл є)
            Map<String, List<DataAnalyticDto>> monthData;
            if (Files.exists(path)) {
                String json = Files.readString(path, StandardCharsets.UTF_8);
                monthData = objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, List<DataAnalyticDto>>>() {});
            } else {
                monthData = new LinkedHashMap<>();
            }

            // 3. БЕЗПЕЧНЕ додавання (використовуємо наш живий список з пам'яті)
            // Ми записуємо в мапу поточний стан нашого списку за сьогодні
            String mapKey = generateDateKey(today, dto.getLocation(), dto.getPowerType());

            // Отримуємо список для цієї локації (він уже наповнений точками в updateDachaGridAnalytic)
            List<DataAnalyticDto> currentList = (dto.getLocation() == LocationType.DACHA)
                    ? this.currentDayGridDachas
                    : this.currentDayGridGolegos;

            // Оновлюємо мапу місяця актуальним списком за сьогодні
            monthData.put(mapKey, new ArrayList<>(currentList));

            // 4. Записуємо ВСЮ мапу місяця (всі дні) назад у файл
            String resultJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(monthData);
            Files.writeString(path, resultJson, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            log.info("Збережено: {}, точок сьогодні: {}", path.getFileName(), currentList.size());

        } catch (IOException e) {
            log.error("Помилка запису аналітики: ", e);
        }
    }

    private synchronized void updateGolegoGridAnalytic() {
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
        long timestamp = System.currentTimeMillis();
        int currentHour = Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .getHour();
        if (deltaKwh > 0) {
            if (currentHour < dayStart || currentHour >= nightStart) {
                this.dailyGridGolegoNight += deltaKwh;
                log.info("Golego Analytic Updated Night: [{}] kWh", this.dailyGridGolegoNight);
            } else {
                this.dailyGridGolegoDay += deltaKwh;
                log.info("Golego Analytic Updated Day: [{}] kWh", this.dailyGridGolegoDay);
            }
        }
        DataAnalyticDto currentDtoGolego = new DataAnalyticDto(LocationType.GOLEGO, PowerType.GRID);
        currentDtoGolego.setPowerNight(this.dailyGridGolegoNight);
        currentDtoGolego.setPowerDay(this.dailyGridGolegoDay);
        currentDtoGolego.setPowerTotal(currentDtoGolego.getPowerDay() + currentDtoGolego.getPowerNight());
        currentDtoGolego.setTimestamp(timestamp);
        this.currentDayGridGolegos.add(currentDtoGolego);
        saveToMonthlyFile(currentDtoGolego);
    }

    private synchronized void updateDachaGridAnalytic() {
        PowerValueRealTimeData powerValueRealTimeData = solarmanTuyaService.getPowerValueRealTimeData();
        double dailyGridPowerCommon = powerValueRealTimeData.getDailyEnergyBuy();
        ZonedDateTime now = ZonedDateTime.now();
        long timestamp = now.toInstant().toEpochMilli(); // мілісекунди
        LocalDate date = now.toLocalDate();              // дата
        int currentHour = now.getHour();
        if (currentHour < dayStart) {
            this.dailyGridDachaNight = dailyGridPowerCommon;
        } else if (currentHour >= nightStart) {
            this.dailyGridDachaNight = dailyGridPowerCommon - this.dailyGridDachaDay;
        } else {
            this.dailyGridDachaDay = dailyGridPowerCommon - this.dailyGridDachaNight;
        }
        DataAnalyticDto currentDtoDacha = new DataAnalyticDto(LocationType.DACHA, PowerType.GRID);
        currentDtoDacha.setTimestamp(timestamp);
        currentDtoDacha.setPowerDay(this.dailyGridDachaDay);
        currentDtoDacha.setPowerNight(this.dailyGridDachaNight);
        currentDtoDacha.setPowerTotal(currentDtoDacha.getPowerDay() + currentDtoDacha.getPowerNight());
        this.currentDayGridDachas.add(currentDtoDacha);
        saveToMonthlyFile(currentDtoDacha);
    }

    private void checkAndResetDay() {
        LocalDate today = LocalDate.now();
        String expectedDachaKey = generateDateKey(today, LocationType.DACHA, PowerType.GRID);
        // ЯКЩО КЛЮЧА ДЛЯ СЬОГОДНІ НЕМАЄ — ЗНАЧИТЬ НОВА ДОБА
        if (!analyticCache.containsKey(expectedDachaKey)) {
            log.info("Нова доба [{}]: скидання кешів аналітики.", expectedDachaKey);

            // 1. Очищаємо кеш (видаляємо похвилинки за минулі дні з RAM)
            analyticCache.clear();

            // 2. Створюємо нові чисті списки
            this.currentDayGridDachas = new ArrayList<>();
            this.currentDayGridGolegos = new ArrayList<>();

            // 3. Реєструємо їх у кеші під новими ключами
            analyticCache.put(expectedDachaKey, this.currentDayGridDachas);

            String expectedGolegoKey = generateDateKey(today, LocationType.GOLEGO, PowerType.GRID);;
            analyticCache.put(expectedGolegoKey, this.currentDayGridGolegos);

            // 4. Скидаємо лічильники (ВАЖЛИВО: інвертор скине свій common о 00:00, ми теж скидаємо свої)
            this.dailyGridDachaDay = 0.0;
            this.dailyGridDachaNight = 0.0;

            log.info("Кеші та накопичувачі успішно скинуті.");
        }
    }

    /**
     * Збереження похвилинної точки (додавання в список)
     */
//    public synchronized void savePoint(DataAnalyticDto point) {
//        String dateKey = generateDateKey(point);
//
//        // Отримуємо існуючий список для цього дня або створюємо новий
//        List<DataAnalyticDto> dayPoints = analyticCache.computeIfAbsent(dateKey, k -> new ArrayList<>());
//
//        // Додаємо нову похвилинну точку
//        dayPoints.add(point);
//
//        // Зберігаємо оновлений файл на диск
//        saveToMonthlyFile(point.getTimestamp());
//    }

    /**
     * Імпорт та Merge з XMLS (від фронта)
     */
    public synchronized List<DataAnalyticDto> importXmlsData(List<DataAnalyticApiDto> list) {
        List<DataAnalyticDto> incomingPoints = DataAnalyticDto.fromApiList(list);
        if (incomingPoints == null || incomingPoints.isEmpty()) return new ArrayList<>();

        // 1. Групуємо всі вхідні точки за ключем "Рік-Місяць" (щоб не відкривати один файл сто разів)
        Map<String, List<DataAnalyticDto>> pointsByMonth = incomingPoints.stream()
                .collect(Collectors.groupingBy(p ->
                        Instant.ofEpochMilli(p.getTimestamp()).atZone(ZoneId.systemDefault())
                                .toLocalDate().format(DateTimeFormatter.ofPattern(patternMonthFile))));

        pointsByMonth.forEach((monthSuffix, points) -> {
            // Беремо першу точку для визначення шляху (локація/тип однакові в пачці зазвичай)
            DataAnalyticDto first = points.get(0);
            Path path = getPathFile(first.getLocation(), first.getPowerType(), monthSuffix);

            try {
                // 2. Читаємо існуючий файл місяця
                Map<String, List<DataAnalyticDto>> monthData = new LinkedHashMap<>();
                if (Files.exists(path)) {
                    String json = Files.readString(path, StandardCharsets.UTF_8);
                    monthData = objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, List<DataAnalyticDto>>>() {});
                }

                // 3. Вклеюємо нові точки в мапу місяця
                for (DataAnalyticDto p : points) {
                    LocalDate pointDate = Instant.ofEpochMilli(p.getTimestamp()).atZone(ZoneId.systemDefault()).toLocalDate();
                    String key = generateDateKey(pointDate, p.getLocation(), p.getPowerType());

                    List<DataAnalyticDto> dayList = monthData.computeIfAbsent(key, k -> new ArrayList<>());

                    // Видаляємо дублікат за таймстампом і додаємо нову
                    dayList.removeIf(old -> old.getTimestamp() == p.getTimestamp());
                    dayList.add(p);
                    dayList.sort(Comparator.comparingLong(DataAnalyticDto::getTimestamp));
                }

                // 4. Пишемо ОНОВЛЕНИЙ МІСЯЦЬ на диск один раз
                Files.createDirectories(path.getParent());
                String resultJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(monthData);
                Files.writeString(path, resultJson, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                log.info("Імпортовано {} точок у файл {}", points.size(), path.getFileName());

                // 5. Якщо імпортували дані за СЬОГОДНІ — оновлюємо живі списки в RAM
                LocalDate today = LocalDate.now();
                String todayKey = generateDateKey(today, first.getLocation(), first.getPowerType());
                if (monthData.containsKey(todayKey)) {
                    if (first.getLocation() == LocationType.DACHA) this.currentDayGridDachas = new ArrayList<>(monthData.get(todayKey));
                    else this.currentDayGridGolegos = new ArrayList<>(monthData.get(todayKey));
                }

            } catch (IOException e) {
                log.error("Помилка імпорту для місяця {}: ", monthSuffix, e);
            }
        });

        return incomingPoints;
    }

    // "yyyy-MM-dd"_dacha_grid
    private String generateDateKey(LocalDate date, LocationType location, PowerType powerType) {
        return date.toString() + separatorKey + location + separatorKey + powerType;
    }

    private LocalDate getDateFromKey(String dateKey) {
        String dateStr = dateKey.split(separatorKey)[0];
        return LocalDate.parse(dateStr);
    }
    // ./analytic/dacha_grid_2026-02.json
    private Path getPathFile(LocationType location, PowerType powerType, String monthSuffix) {
        return Paths.get(dirAnalytic, location.name() + separatorKey + powerType.name() + separatorKey + monthSuffix + ".json");
    }
}
