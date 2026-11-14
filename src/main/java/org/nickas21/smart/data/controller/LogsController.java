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
public class LogsController extends BaseController {

    private final LogsService logsService;

    @GetMapping("/app")
    public String getLogsFromAppTerminal() throws IOException {
        return logsService.getLogsApp();
    }

    @GetMapping("/app/current")
    public String getLogsFromAppCurrent() {
        return logsService.getLogsAppCurrent();
    }

    @GetMapping("/app/previous")
    public String getLogsFromAppPrevious() {
        return logsService.getLogsAppPrevious();
    }
}