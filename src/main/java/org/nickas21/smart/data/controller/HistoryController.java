package org.nickas21.smart.data.controller;

import org.nickas21.smart.data.dataEntityDto.HistoryDto;
import org.nickas21.smart.data.service.HistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/history")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/golego/today")
    public ResponseEntity<List<HistoryDto>> getInfoHomeGolegoToday() {
        return ResponseEntity.ok(this.historyService.getHistoryGolegoToday());
    }

    @GetMapping("/dacha/today")
    public ResponseEntity<List<HistoryDto>> getInfoHomeDachaToday() {
        return ResponseEntity.ok(this.historyService.getHistoryDachaToday());
    }

    @GetMapping("/golego/yesterday")
    public ResponseEntity<List<HistoryDto>> getInfoHomeGolegoYesterday() {
        return ResponseEntity.ok(this.historyService.getHistoryGolegoYesterday());
    }

    @GetMapping("/dacha/yesterday")
    public ResponseEntity<List<HistoryDto>> getInfoHomeDachaYesterday() {
        return ResponseEntity.ok(this.historyService.getHistoryDachaYesterday());
    }
}
