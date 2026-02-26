package org.nickas21.smart.data.controller;

import org.nickas21.smart.data.dataEntityDto.DataAnalyticDto;
import org.nickas21.smart.data.dataEntityDto.PowerType;
import org.nickas21.smart.data.service.AnalyticService;
import org.nickas21.smart.util.LocationType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth; // Потрібен цей імпорт
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping(value = "/api/analytic")
public class AnalyticController {

    private final AnalyticService analyticService;

    public AnalyticController(AnalyticService analyticService) {
        this.analyticService = analyticService;
    }

    @GetMapping("/current")
    public ResponseEntity<DataAnalyticDto> getGolegoAnalyticDayCurrent(@RequestParam String locationType, @RequestParam String powerType) {
        return ResponseEntity.ok(this.analyticService.getAnalyticByDay(LocalDate.now(), LocationType.getByName(locationType), PowerType.getByName(powerType)));
    }

    @GetMapping("/day")
    public ResponseEntity<DataAnalyticDto> getGolegoAnalyticDay(
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

    @GetMapping("/month")
    public ResponseEntity<List<DataAnalyticDto>> getDachaAnalyticMonth(
            // ВИПРАВЛЕНО: Для yyyy-MM використовуємо YearMonth
            @RequestParam @DateTimeFormat(pattern = AnalyticService.patternMonthFile) YearMonth date,
            @RequestParam String locationType,
            @RequestParam String powerType) {
        String yearMonthStr = date.format(DateTimeFormatter.ofPattern(AnalyticService.patternMonthFile));
        return ResponseEntity.ok(
                this.analyticService.getAnalyticForMonth(
                        yearMonthStr,
                        LocationType.getByName(locationType),
                        PowerType.getByName(powerType)
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
}