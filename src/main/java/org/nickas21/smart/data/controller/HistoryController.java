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

    @GetMapping("/golego")
    public ResponseEntity<List<HistoryDto>> getInfoHomeGolego() {
        return ResponseEntity.ok(this.historyService.getHistoryGolego());
    }

    @GetMapping("/dacha")
    public ResponseEntity<List<HistoryDto>> getInfoHomeDacha() {
        return ResponseEntity.ok(this.historyService.getHistoryDacha());
    }
}
