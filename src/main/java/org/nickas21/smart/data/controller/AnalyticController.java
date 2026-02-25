package org.nickas21.smart.data.controller;

import org.nickas21.smart.data.dataEntityDto.DataErrorInfoDto;
import org.nickas21.smart.data.service.AnalyticService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/analytic")
public class AnalyticController {

    private final AnalyticService analyticService;

    public AnalyticController(AnalyticService analyticService) {
        this.analyticService = analyticService;
    }

    @GetMapping("/golego")
    public ResponseEntity<List<DataErrorInfoDto>> getGolegoAlarms() {
        // Повертає список з файлу GolegoErrors
        return ResponseEntity.ok(this.analyticService.getGolegoAnalyticDayCurrent());
    }

    @GetMapping("/dacha")
    public ResponseEntity<List<DataErrorInfoDto>> getDachaAlarms() {
        // Повертає список з файлу DachaErrors
        return ResponseEntity.ok(this.analyticService.getDachaAnalyticDayCurrent());
    }

}
