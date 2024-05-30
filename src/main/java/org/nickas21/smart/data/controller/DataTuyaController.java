package org.nickas21.smart.data.controller;

import org.nickas21.smart.data.service.UserService;
import org.nickas21.smart.tuya.TuyaDeviceService;
import org.nickas21.smart.tuya.tuyaEntity.Devices;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/api/tuya")
public class DataTuyaController {

    private final UserService userService;
    private final TuyaDeviceService tuyaDeviceService;

    public DataTuyaController(UserService userService, TuyaDeviceService tuyaDeviceService) {
        this.userService = userService;
        this.tuyaDeviceService = tuyaDeviceService;
    }


    @GetMapping("/dev")
    public Mono<ResponseEntity<Devices>> getConfig(@RequestHeader("Authorization") String token) {
        return userService.validateToken(token)
                .flatMap(isValid -> {
                    if (!isValid) {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(new Devices("Invalid token")));
                    }
                    if (tuyaDeviceService.devices != null) {
                        return Mono.just(ResponseEntity.ok(this.tuyaDeviceService.devices));
                    } else {
                        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(new Devices("Devices not found")));
                    }
                })
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new Devices("An error occurred: " + e.getMessage()))));
    }

}
