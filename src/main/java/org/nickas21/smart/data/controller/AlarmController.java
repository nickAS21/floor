package org.nickas21.smart.data.controller;

import org.nickas21.smart.data.dataEntityDto.DataErrorInfoDto;
import org.nickas21.smart.data.service.AlarmService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/alarms")
public class AlarmController {

    private final AlarmService alarmService;

    public AlarmController(AlarmService alarmService) {
        this.alarmService = alarmService;
    }

    @GetMapping("/golego")
    public ResponseEntity<List<DataErrorInfoDto>> getGolegoAlarms() {
        // Повертає список з файлу GolegoErrors
        return ResponseEntity.ok(this.alarmService.getGolegoErrors());
    }

    @GetMapping("/dacha")
    public ResponseEntity<List<DataErrorInfoDto>> getDachaAlarms() {
        // Повертає список з файлу DachaErrors
        return ResponseEntity.ok(this.alarmService.getDachaErrors());
    }

    @DeleteMapping("/golego/clear")
    public ResponseEntity<Void> clearGolegoAlarms() {
        // Можливість очистити список помилок (якщо потрібно)
        this.alarmService.clearGolegoErrors();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/dacha/clear")
    public ResponseEntity<Void> clearDachaAlarms() {
        // Можливість очистити список помилок (якщо потрібно)
        this.alarmService.clearDachaErrors();
        return ResponseEntity.noContent().build();
    }
}