package org.nickas21.smart.data.controller;

import org.nickas21.smart.data.dataEntity.BatteryCellInfo;
import org.nickas21.smart.data.service.LogsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/logs")
public class LogsController extends BaseController{

    private final LogsService logsService;

    public LogsController(LogsService logsService) {
        this.logsService = logsService;
    }

    @GetMapping("/golego")
    public ResponseEntity<BatteryCellInfo> getLogsHomeGolego() throws IOException {
        return ResponseEntity.ok(this.logsService.getLogsGolego());
    }

    @GetMapping("/dacha")
    public ResponseEntity<Map<String, String>> getLogsHomeDacha() throws IOException {
        return ResponseEntity.ok(Map.of("data", this.logsService.getLogsDacha()));
    }
}
