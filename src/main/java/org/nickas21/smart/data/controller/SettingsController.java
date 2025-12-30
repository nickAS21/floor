package org.nickas21.smart.data.controller;

import org.nickas21.smart.data.dataEntityDto.DataSettingsDto;
import org.nickas21.smart.data.service.SettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/golego")
    public ResponseEntity<DataSettingsDto> getSettingsGolego() {
            return ResponseEntity.ok(this.settingsService.getSettingsGolego());
    }

    @GetMapping("/dacha")
    public ResponseEntity<DataSettingsDto> getSettingsDacha() {
            return ResponseEntity.ok(this.settingsService.getSettingsDacha());
    }

    @PostMapping("/golego")
    public ResponseEntity<DataSettingsDto> postSettingsGolego(@RequestBody DataSettingsDto settingsGolego) {
        // Передаємо отримані з фронтенду налаштування в сервіс
        return ResponseEntity.ok(this.settingsService.setSettingsGolego(settingsGolego));
    }

    @PostMapping("/dacha")
    public ResponseEntity<DataSettingsDto> postSettingsDacha(@RequestBody DataSettingsDto settingsDacha) {
        // Передаємо отримані з фронтенду налаштування в сервіс
        return ResponseEntity.ok(this.settingsService.setSettingsDacha(settingsDacha));
    }
}

