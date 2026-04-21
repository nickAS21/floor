package org.nickas21.smart.data.controller;

import org.nickas21.smart.data.dataEntityDto.DataHomeDto;
import org.nickas21.smart.data.service.AnalyticService; // 1. Міняємо імпорт
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/home")
public class DataHomeController extends BaseController {

    // 2. Міняємо сервіс на AnalyticService
    private final AnalyticService analyticService;

    public DataHomeController(AnalyticService analyticService) {
        this.analyticService = analyticService;
    }

    @GetMapping("/golego")
    public ResponseEntity<DataHomeDto> getInfoHomeGolego() {
        // 3. Викликаємо метод збагачення даних
        return ResponseEntity.ok(this.analyticService.getEnrichedDataGolego());
    }

    @GetMapping("/dacha")
    public ResponseEntity<DataHomeDto> getInfoHomeDacha() {
        // 4. Викликаємо метод збагачення даних
        return ResponseEntity.ok(this.analyticService.getEnrichedDataDacha());
    }
}