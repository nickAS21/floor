package org.nickas21.smart.data.controller;

import org.nickas21.smart.data.service.SettingsService;
import org.nickas21.smart.data.service.UserService;
import org.nickas21.smart.tuya.TuyaConnectionProperties;
import org.nickas21.smart.tuya.TuyaDeviceService;
import org.nickas21.smart.tuya.tuyaEntity.Devices;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/smart")
public class SettingsController {

    private final SettingsService settingsService;

    private final UserService userService;
    private final TuyaDeviceService tuyaDeviceService;
    private final TuyaConnectionProperties tuyaConnectionProperties;

    public SettingsController(SettingsService settingsService, UserService userService, TuyaDeviceService tuyaDeviceService, TuyaConnectionProperties tuyaConnectionProperties) {
        this.settingsService = settingsService;
        this.userService = userService;
        this.tuyaDeviceService = tuyaDeviceService;
        this.tuyaConnectionProperties = tuyaConnectionProperties;
    }



    @GetMapping("/config")
    public ResponseEntity<Devices> getConfig(
            @RequestHeader(required = false, value = "Authorization") String token) {

        if (!userService.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (tuyaDeviceService.devices != null) {
            return ResponseEntity.ok(this.tuyaDeviceService.devices);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Devices("Config not found"));
        }
    }

    @GetMapping("/logs")
    public ResponseEntity<Devices> getLogs(
            @RequestHeader(required = false, value = "Authorization") String token) {

        if (!userService.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (tuyaDeviceService.devices != null) {
            return ResponseEntity.ok(this.tuyaDeviceService.devices);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Devices("Config not found"));
        }
    }

}
