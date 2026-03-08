package org.nickas21.smart.data.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.data.dataEntityDto.DataAnalytic;
import org.nickas21.smart.data.dataEntityDto.DataAnalyticDto;
import org.nickas21.smart.data.dataEntityDto.DataAnalyticResponse;
import org.nickas21.smart.data.service.AnalyticService;
import org.nickas21.smart.util.LocationType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/api/analytic")
public class AnalyticController {

    private final AnalyticService analyticService;

    public AnalyticController(AnalyticService analyticService) {
        this.analyticService = analyticService;
    }

    @GetMapping("/day")
    public ResponseEntity<DataAnalyticResponse> getGolegoAnalyticDay(
            @RequestParam @DateTimeFormat(pattern = AnalyticService.patternDayKey) LocalDate date,
            @RequestParam String locationType) {

        LocationType location = LocationType.getByName(locationType);
        List<DataAnalyticDto> dtos = this.analyticService.getAnalyticByDay(date, location);
        return ResponseEntity.ok(new DataAnalyticResponse(
                location.getZoneId().getId(),
                dtos
        ));
    }

    @GetMapping("/days")
    public  ResponseEntity<DataAnalyticResponse> getGolegoAnalyticDays(
            @RequestParam @DateTimeFormat(pattern = AnalyticService.patternDayKey) LocalDate dateStart,
            @RequestParam @DateTimeFormat(pattern = AnalyticService.patternDayKey) LocalDate dateFinish,
            @RequestParam String locationType) {
        LocationType location = LocationType.getByName(locationType);
        List<DataAnalyticDto> dtos = this.analyticService.loadDtosForDates(
                dateStart,
                dateFinish,
                LocationType.getByName(locationType)
        );
        return ResponseEntity.ok(new DataAnalyticResponse(
                location.getZoneId().getId(),
                dtos
        ));
    }

    @GetMapping("/month")
    public ResponseEntity<DataAnalyticResponse> getDachaAnalyticMonth(
            @RequestParam @DateTimeFormat(pattern = AnalyticService.patternMonthFile) YearMonth date,
            @RequestParam String locationType) {
        LocationType location = LocationType.getByName(locationType);
        String monthSuffix = date.format(DateTimeFormatter.ofPattern(AnalyticService.patternMonthFile));
        List<DataAnalyticDto> dtos = this.analyticService.getAnalyticForMonth(location, monthSuffix);

        return ResponseEntity.ok(new DataAnalyticResponse(
                location.getZoneId().getId(),
                dtos
        ));
    }

    @GetMapping("/year")
    public ResponseEntity<DataAnalyticResponse> getAnalyticDayYear(
            @RequestParam @DateTimeFormat(pattern = AnalyticService.patternYearFile) Year year,
            @RequestParam String locationType) {
        LocationType location = LocationType.getByName(locationType);

        List<DataAnalyticDto> dtos = this.analyticService.getAnalyticForYear(
                year.getValue(),
                location
        );
        return ResponseEntity.ok(new DataAnalyticResponse(
                location.getZoneId().getId(),
                dtos
        ));
    }

    @PostMapping("/import/xmls")
    public ResponseEntity<?> importXmlsData(@RequestBody String rawJson) {
        // 1. Логуємо вхідний JSON, щоб бачити регістр
        log.debug("Recieved JSON for import: {}", rawJson);

        try {
            ObjectMapper mapper = new ObjectMapper();
            // Це дозволить Jackson ігнорувати регістр Енумів автоматично
            mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);

            List<DataAnalytic> list = mapper.readValue(rawJson, new TypeReference<List<DataAnalytic>>(){});
            return ResponseEntity.ok(this.analyticService.importXmlsData(list));
        } catch (Exception e) {
            // 2. Логуємо конкретну причину 400 помилки
            log.error("JSON Mapping Error: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}