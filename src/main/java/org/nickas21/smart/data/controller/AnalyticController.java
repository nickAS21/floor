package org.nickas21.smart.data.controller;

import org.nickas21.smart.data.dataEntityDto.DataAnalyticApiDto;
import org.nickas21.smart.data.dataEntityDto.DataAnalyticDto;
import org.nickas21.smart.data.dataEntityDto.PowerType;
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

@RestController
@RequestMapping(value = "/api/analytic")
public class AnalyticController {

    private final AnalyticService analyticService;

    public AnalyticController(AnalyticService analyticService) {
        this.analyticService = analyticService;
    }

    @GetMapping("/day")
    public ResponseEntity<List<DataAnalyticDto>> getGolegoAnalyticDay(
            @RequestParam @DateTimeFormat(pattern = AnalyticService.patternDayKey) LocalDate date,
            @RequestParam String locationType,
            @RequestParam String powerType) {
        // ВИПРАВЛЕНО: Використовуємо getAnalyticByDay, щоб задіяти кеш для "сьогодні"
        return ResponseEntity.ok(
                this.analyticService.getAnalyticByDay(
                        date,
                        LocationType.getByName(locationType),
                        PowerType.getByName(powerType)
                ));
    }

    @GetMapping("/days")
    public ResponseEntity<List<DataAnalyticDto>> getGolegoAnalyticDays(
            @RequestParam @DateTimeFormat(pattern = AnalyticService.patternDayKey) LocalDate dateStart,
            @RequestParam @DateTimeFormat(pattern = AnalyticService.patternDayKey) LocalDate dateFinish,
            @RequestParam String locationType,
            @RequestParam String powerType) {
        return ResponseEntity.ok(
                this.analyticService.loadDtosForDates(
                        dateStart,
                        dateFinish,
                        LocationType.getByName(locationType),
                        PowerType.getByName(powerType)
                ));
    }

    @GetMapping("/month")
    public ResponseEntity<List<DataAnalyticDto>> getDachaAnalyticMonth(
            // ВИПРАВЛЕНО: Для yyyy-MM використовуємо YearMonth
            @RequestParam @DateTimeFormat(pattern = AnalyticService.patternMonthFile) YearMonth date,
            @RequestParam String locationType,
            @RequestParam String powerType) {
        String monthSuffix = date.format(DateTimeFormatter.ofPattern(AnalyticService.patternMonthFile));
        return ResponseEntity.ok(
                this.analyticService.getAnalyticForMonth(
                        LocationType.getByName(locationType),
                        PowerType.getByName(powerType),
                        monthSuffix
                ));
    }

    @GetMapping("/year")
    public ResponseEntity<List<DataAnalyticDto>> getAnalyticDayYear(
            @RequestParam @DateTimeFormat(pattern = AnalyticService.patternYearFile) Year year,
            @RequestParam String locationType,
            @RequestParam String powerType) {

        return ResponseEntity.ok(this.analyticService.getAnalyticForYear(
                year.getValue(),
                LocationType.getByName(locationType),
                PowerType.getByName(powerType)
        ));
    }

    @PostMapping("/import/xmls")
    public ResponseEntity<List<DataAnalyticDto>> importXmlsData(
            @RequestBody List<DataAnalyticApiDto> list)
    {
        return ResponseEntity.ok(this.analyticService.importXmlsData(list));
    }
}