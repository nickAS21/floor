package org.nickas21.smart.data.controller;

import org.nickas21.smart.data.dataEntityDto.DataUsrWiFiInfoDto;
import org.nickas21.smart.data.service.UsrWiFiInfoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/provision")
public class UsrWiFiInfoController {

    private final UsrWiFiInfoService usrWiFiInfoService;

    public UsrWiFiInfoController(UsrWiFiInfoService usrWiFiInfoService) {
        this.usrWiFiInfoService = usrWiFiInfoService;
    }

    @GetMapping("/golego")
    public ResponseEntity<DataUsrWiFiInfoDto> getUsrWiFiInfoGolego() {
        return ResponseEntity.ok(this.usrWiFiInfoService.getUsrWiFiInfoGolego());
    }

    @GetMapping("/dacha")
    public ResponseEntity<DataUsrWiFiInfoDto> getUsrWiFiInfoDacha() {
        return ResponseEntity.ok(this.usrWiFiInfoService.getUsrWiFiInfoDacha());
    }

    @PostMapping("/golego")
    public ResponseEntity<DataUsrWiFiInfoDto> postUsrWiFiInfoGolego(@RequestBody DataUsrWiFiInfoDto usrWiFiInfoGolego) {
        // Передаємо отримані з фронтенду налаштування в сервіс
        return ResponseEntity.ok(this.usrWiFiInfoService.setUsrWiFiInfolego(usrWiFiInfoGolego));
    }

    @PostMapping("/dacha")
    public ResponseEntity<DataUsrWiFiInfoDto> postUsrWiFiInfoDacha(@RequestBody DataUsrWiFiInfoDto usrWiFiInfoDacha) {
        // Передаємо отримані з фронтенду налаштування в сервіс
        return ResponseEntity.ok(this.usrWiFiInfoService.setUsrWiFiInfoDacha(usrWiFiInfoDacha));
    }


}
