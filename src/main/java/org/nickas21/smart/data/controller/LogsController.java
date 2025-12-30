package org.nickas21.smart.data.controller;

import lombok.RequiredArgsConstructor;
import org.nickas21.smart.data.service.LogsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping(value = "/api/logs")
@RequiredArgsConstructor
public class LogsController extends BaseController{

    private final LogsService logsService;


//    @GetMapping("/golego")
//    public ResponseEntity<BatteryCellInfo> getLogsHomeGolego() throws IOException {
//        return ResponseEntity.ok(this.logsService.getLogsGolego());
//    }

    @GetMapping("/dacha")
    public String getLogsHomeDacha() throws IOException {
        return logsService.getLogsDacha();
    }
}
