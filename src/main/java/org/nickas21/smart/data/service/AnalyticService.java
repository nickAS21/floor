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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
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

    private final DataHomeService dataHomeService;
    private final UsrTcpWiFiParseData usrTcpWiFiParseData;
    private final TuyaDeviceService deviceService;
    private final DefaultSmartSolarmanTuyaService solarmanTuyaService;

    private List<DataAnalyticDto> currentDayGolegos;
    private List<DataAnalyticDto> currentDayDachas;
    DataAnalyticDto lastAnalyticDtoGolego;
    DataAnalyticDto lastAnalyticDtoDacha;
    public static final String patternYearFile = "yyyy";
    public static final String patternMonthFile = "yyyy-MM";
    public static final String patternDayKey = "yyyy-MM-dd";
    private static final String separatorKey = "_";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        LocalDate localDate = this.getLocalDateInverter();
        this.currentDayGolegos = loadDtosForDate(localDate, LocationType.GOLEGO);
        this.currentDayDachas = loadDtosForDate(localDate, LocationType.DACHA);

        this.analyticCache.put(generateDateKey(localDate, LocationType.GOLEGO), this.currentDayGolegos);
        this.analyticCache.put(generateDateKey(localDate, LocationType.DACHA), this.currentDayDachas);
        processLastPoint(this.currentDayDachas, LocationType.DACHA);
        processLastPoint(this.currentDayGolegos, LocationType.GOLEGO);
    }

    private void processLastPoint(List<DataAnalyticDto> list, LocationType type) {
        if (!list.isEmpty()) {
            list.sort(Comparator.comparingLong(DataAnalyticDto::getTimestamp));
            if (type == LocationType.DACHA) this.lastAnalyticDtoDacha = list.getLast();
            else this.lastAnalyticDtoGolego = list.getLast();
        } else {
            if (type == LocationType.DACHA) this.lastAnalyticDtoDacha = new DataAnalyticDto(type);
            else this.lastAnalyticDtoGolego = new DataAnalyticDto(type);
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
        checkAndResetDay();
        updateDachaAnalytic();
        updateGolegoAnalytic();
        LocalDate date = this.getLocalDateInverter();
        this.analyticCache.put(generateDateKey(date, LocationType.GOLEGO), this.currentDayGolegos);
        this.analyticCache.put(generateDateKey(date, LocationType.DACHA), this.currentDayDachas);
    }

    public List<DataAnalyticDto> getAnalyticByDay(LocalDate date, LocationType location) {
        LocalDate today = this.getLocalDateInverter();
        if (today.equals(date)) {
            if (location == LocationType.DACHA) {
                return this.currentDayDachas;
            } else if (location == LocationType.GOLEGO) {
                return this.currentDayGolegos;
            }
        }
        return loadDtosForDate(date, location);
    }

    public List<DataAnalyticDto> loadDtosForDate(LocalDate date, LocationType location) {
        String monthSuffix = date.format(DateTimeFormatter.ofPattern(patternMonthFile));
        String mapKey = generateDateKey(date, location);
        Path path = getPathFile(location, monthSuffix);

        if (this.analyticCache.containsKey(mapKey)) {
            return this.analyticCache.get(mapKey);
        }

        if (Files.exists(path)) {
            try {
                String content = Files.readString(path, StandardCharsets.UTF_8);
                Map<String, List<DataAnalyticDto>> monthlyMap = objectMapper.readValue(content, new TypeReference<>() {});
                if (monthlyMap != null && monthlyMap.containsKey(mapKey)) {
                    List<DataAnalyticDto> dayPoints = monthlyMap.get(mapKey);
                    this.analyticCache.put(mapKey, dayPoints);
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
            String dayKey = generateDateKey(current, location);

            if (analyticCache.containsKey(dayKey)) {
                result.addAll(analyticCache.get(dayKey));
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
                if (monthlyMap.containsKey(dayKey)) {
                    result.addAll(monthlyMap.get(dayKey));
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
        LocalDate today = this.getLocalDateInverter();
        String monthSuffix = today.format(DateTimeFormatter.ofPattern(patternMonthFile));
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
            String mapKey = generateDateKey(today, dto.getLocation());
            List<DataAnalyticDto> currentList = (dto.getLocation() == LocationType.DACHA) ? this.currentDayDachas : this.currentDayGolegos;
            monthData.put(mapKey, new ArrayList<>(currentList));
            String resultJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(monthData);
            Files.writeString(path, resultJson, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.error("Помилка запису: ", e);
        }
    }

    private void checkAndResetDay() {
        LocalDate today = this.getLocalDateInverter();
        String expectedDachaKey = generateDateKey(today, LocationType.DACHA);
        if (!analyticCache.containsKey(expectedDachaKey)) {
            analyticCache.clear();
            this.currentDayDachas = Collections.synchronizedList(new ArrayList<>());
            this.currentDayGolegos = Collections.synchronizedList(new ArrayList<>());
            this.lastAnalyticDtoDacha = new DataAnalyticDto(LocationType.DACHA);
            this.lastAnalyticDtoGolego = new DataAnalyticDto(LocationType.GOLEGO);
            analyticCache.put(expectedDachaKey, this.currentDayDachas);
            analyticCache.put(generateDateKey(today, LocationType.GOLEGO), this.currentDayGolegos);
        }
    }

    private synchronized void updateDachaAnalytic() {
        PowerValueRealTimeData powerValueRealTimeData = solarmanTuyaService.getPowerValueRealTimeData();
        double dailyGridPowerCommon = powerValueRealTimeData.getDailyEnergyBuy();
        long timestamp = powerValueRealTimeData.getCollectionTime() * 1000;
        int currentHour = getLocalDateInverterHour(timestamp);
        double dailyGridDachaNight = this.lastAnalyticDtoDacha.getGridDailyNightPower();
        double dailyGridDachaDay = this.lastAnalyticDtoDacha.getGridDailyDayPower();
        if (currentHour < dayStart) dailyGridDachaNight = dailyGridPowerCommon;
        else if (currentHour >= nightStart) dailyGridDachaNight = dailyGridPowerCommon - dailyGridDachaDay;
        else dailyGridDachaDay = dailyGridPowerCommon - dailyGridDachaNight;
        DataAnalyticDto currentDtoDacha = new DataAnalyticDto(LocationType.DACHA);
        currentDtoDacha.setTimestamp(timestamp);
        currentDtoDacha.setGridDailyDayPower(dailyGridDachaDay);
        currentDtoDacha.setGridDailyNightPower(dailyGridDachaNight);
        currentDtoDacha.setGridDailyTotalPower(dailyGridDachaDay + dailyGridDachaNight);
        currentDtoDacha.setBmsSoc(powerValueRealTimeData.getBatterySocValue());
        currentDtoDacha.setSolarDailyPower(powerValueRealTimeData.getDailyProductionSolarPower());
        currentDtoDacha.setHomeDailyPower(powerValueRealTimeData.getDailyHomeConsumptionPower());

        currentDtoDacha.setGridPower(powerValueRealTimeData.getTotalGridPower());
        currentDtoDacha.setSolarPower(powerValueRealTimeData.getTotalProductionSolarPower());
        currentDtoDacha.setHomePower(powerValueRealTimeData.getTotalHomePower());
        currentDtoDacha.setBmsDailyDischarge(powerValueRealTimeData.getDailyBatteryDischarge());
        currentDtoDacha.setBmsDailyCharge(powerValueRealTimeData.getDailyBatteryCharge());

        this.currentDayDachas.add(currentDtoDacha);
        saveToMonthlyFile(currentDtoDacha);
        this.lastAnalyticDtoDacha = currentDtoDacha;
    }

    private synchronized void updateGolegoAnalytic() {
        double currentGridPower = 0;
        UsrTcpWiFiBatteryRegistry registry = usrTcpWiFiParseData.getUsrTcpWiFiBatteryRegistry();
        Integer port = usrTcpWiFiParseData.getUsrTcpWiFiProperties().getPortInverterGolego();
        InverterData data = registry.getInverter(port);
        Instant timestampInstant = data.getLastTime();
        if (Boolean.TRUE.equals(deviceService.getGridRelayCodeGolegoStateSwitch()) && Boolean.TRUE.equals(deviceService.getGridRelayCodeGolegoStateOnLine())) {
            if (data.getInvertorGolegoData90() != null) {
                InvertorGolegoData90 d90 = data.getInvertorGolegoData90();
                currentGridPower = d90.getBatteryVoltage() * d90.getBatteryCurrent() + d90.getLoadOutputActivePower() + golegoInverterPowerDefault;
            }
        }
        double deltaKwh = (currentGridPower / 1000.0) * (updateRateMs / 3600000.0);
        int currentHour = getLocalDateInverterHour(timestampInstant.toEpochMilli());
        double dailyGridGolegoNight = this.lastAnalyticDtoGolego.getGridDailyNightPower();
        double dailyGridGolegoDay = this.lastAnalyticDtoGolego.getGridDailyDayPower();
        if (deltaKwh > 0) {
            if (currentHour < dayStart || currentHour >= nightStart) dailyGridGolegoNight += deltaKwh;
            else dailyGridGolegoDay += deltaKwh;
        }
        DataHomeDto dataHomeDto = dataHomeService.getDataGolego();
        DataAnalyticDto currentDtoGolego = new DataAnalyticDto(LocationType.GOLEGO);
        currentDtoGolego.setTimestamp(timestampInstant.toEpochMilli());
        currentDtoGolego.setGridDailyDayPower(dailyGridGolegoDay);
        currentDtoGolego.setGridDailyNightPower(dailyGridGolegoNight);
        currentDtoGolego.setGridDailyTotalPower(dailyGridGolegoDay + dailyGridGolegoNight);
        currentDtoGolego.setBmsSoc(this.lastAnalyticDtoGolego.getBmsSoc() + dataHomeDto.getBatterySoc());
        currentDtoGolego.setHomeDailyPower(this.lastAnalyticDtoGolego.getHomeDailyPower() + dataHomeDto.getHomePower());
        currentDtoGolego.setSolarDailyPower(0);
        this.currentDayGolegos.add(currentDtoGolego);
        saveToMonthlyFile(currentDtoGolego);
        this.lastAnalyticDtoGolego = currentDtoGolego;
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
                    String key = generateDateKey(pointDate, p.getLocation());
                    List<DataAnalytic> dayList = monthData.computeIfAbsent(key, k -> new ArrayList<>());
                    dayList.removeIf(old -> old.getTimestamp() == p.getTimestamp());
                    dayList.add(p);
                    dayList.sort(Comparator.comparingLong(DataAnalytic::getTimestamp));
                }
                Files.createDirectories(path.getParent());
                Files.writeString(path, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(monthData), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                LocalDate today = this.getLocalDateInverter();
                String todayKey = generateDateKey(today, first.getLocation());
                if (monthData.containsKey(todayKey)) {
                    List<DataAnalyticDto> dtoList = monthData.get(todayKey).stream()
                            .map(e -> new DataAnalyticDto(e.getTimestamp(), e.getLocation(), e.getGridDailyDayPower(), e.getGridDailyNightPower(), e.getGridDailyTotalPower()))
                            .collect(Collectors.toList());
                    if (first.getLocation() == LocationType.DACHA) this.currentDayDachas = dtoList;
                    else this.currentDayGolegos = dtoList;
                }
            } catch (IOException e) { log.error("Error import: ", e); }
        });
        return incomingPoints;
    }

    private String generateDateKey(LocalDate date, LocationType location) {
        return date.toString() + separatorKey + location;
    }

    private Path getPathFile(LocationType location, String monthSuffix) {
        return Paths.get(dirAnalytic, location.name() + separatorKey + monthSuffix + ".json");
    }


    private LocalDate getLocalDateInverter() {
        return Instant.now()
                .atZone(LocationType.GOLEGO.getZoneId())
                .toLocalDate();
    }

    private int getLocalDateInverterHour(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
                .atZone(LocationType.GOLEGO.getZoneId())
                .getHour();
    }
}