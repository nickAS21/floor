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

@RestController
@RequestMapping(value = "/api/unit")
public class DataUnitController {

    private final UserService userService;
    private final TuyaDeviceService tuyaDeviceService;

    public DataUnitController(UserService userService, TuyaDeviceService tuyaDeviceService) {
        this.userService = userService;
        this.tuyaDeviceService = tuyaDeviceService;
    }


    @GetMapping("/dacha")
    public ResponseEntity<Devices> getConfig(
            @RequestHeader(required = false, value = "Authorization") String token) {

        if (!userService.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (tuyaDeviceService.getDevices() != null) {
            return ResponseEntity.ok(this.tuyaDeviceService.getDevices());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Devices("Devices not found"));
        }
      }

}
