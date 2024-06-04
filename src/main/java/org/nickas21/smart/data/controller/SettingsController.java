package org.nickas21.smart.data.controller;

import org.nickas21.smart.data.service.SettingsService;
import org.nickas21.smart.data.service.UserService;
import org.nickas21.smart.tuya.TuyaConnectionProperties;
import org.nickas21.smart.tuya.TuyaDeviceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

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
    public Mono<ResponseEntity<String[]>> getConfig(@RequestHeader(required = false, value = "Authorization") String token) {
        return userService.validateToken(token)
                .flatMap(isValid -> {
                    if (isValid) {
                        if (this.tuyaDeviceService.devices != null) {
                            return Mono.just(ResponseEntity.ok(this.tuyaConnectionProperties.getDeviceIds()));
                        } else {
                            return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                    .body(new String[]{"Devices not found"}));
                        }
                    } else {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(new String[]{"Invalid token"}));
                    }
                })
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new String[]{"An error occurred: " + e.getMessage()})));
    }

}
