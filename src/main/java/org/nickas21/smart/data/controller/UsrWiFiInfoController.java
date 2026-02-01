package org.nickas21.smart.data.controller;
import org.nickas21.smart.data.dataEntityDto.DataUsrWiFiInfoDto;
import org.nickas21.smart.data.service.UsrWiFiInfoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/provision")
public class UsrWiFiInfoController {

    private final UsrWiFiInfoService usrWiFiInfoService;

    public UsrWiFiInfoController(UsrWiFiInfoService usrWiFiInfoService) {
        this.usrWiFiInfoService = usrWiFiInfoService;
    }

    @GetMapping("/golego")
    public ResponseEntity<List<DataUsrWiFiInfoDto>> getUsrWiFiInfoGolego() {
        // Повертає повний список об'єктів для локації Golego
        return ResponseEntity.ok(this.usrWiFiInfoService.getUsrWiFiInfoGolego());
    }

    @GetMapping("/dacha")
    public ResponseEntity<List<DataUsrWiFiInfoDto>> getUsrWiFiInfoDacha() {
        // Повертає повний список об'єктів для локації Dacha
        return ResponseEntity.ok(this.usrWiFiInfoService.getUsrWiFiInfoDacha());
    }

    @PostMapping("/golego")
    public ResponseEntity<List<DataUsrWiFiInfoDto>> postUsrWiFiInfoGolego(@RequestBody List<DataUsrWiFiInfoDto> list) {
        // Приймає список від фронтенда, оновлює кеш та файл
        return ResponseEntity.ok(this.usrWiFiInfoService.setUsrWiFiInfolego(list));
    }

    @PostMapping("/dacha")
    public ResponseEntity<List<DataUsrWiFiInfoDto>> postUsrWiFiInfoDacha(@RequestBody List<DataUsrWiFiInfoDto> list) {
        // Приймає список від фронтенда, оновлює кеш та файл
        return ResponseEntity.ok(this.usrWiFiInfoService.setUsrWiFiInfoDacha(list));
    }
}