package org.nickas21.smart.data.controller;

import org.nickas21.smart.data.dataEntity.DataHome;
import org.nickas21.smart.data.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/home")
public class DataHomeController {

    private final UserService userService;
//    private final TuyaDeviceService tuyaDeviceService;
//    private final TuyaConnectionProperties tuyaConnectionProperties;

//    public DataHomeController(UserService userService, TuyaDeviceService tuyaDeviceService, TuyaConnectionProperties tuyaConnectionProperties) {
    public DataHomeController(UserService userService) {
        this.userService = userService;
//        this.tuyaDeviceService = tuyaDeviceService;
//        this.tuyaConnectionProperties = tuyaConnectionProperties;
    }


    @GetMapping("/golego")
    public ResponseEntity<DataHome> getInfoHomeGolego(
            @RequestHeader(required = false, value = "Authorization") String token) {

        if (!userService.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        DataHome data = new DataHome();
        data.setTimestamp(System.currentTimeMillis());
        data.setBatterySoc(85.5);
        data.setBatteryStatus("CHARGING");
        data.setBatteryVol(52.3);
        data.setBatteryCurrent(10.2);
        data.setGridStatusRealTime(true);
        data.setSolarPower(3200.0);

        return ResponseEntity.ok(data);
    }

    @GetMapping("/dacha")
    public ResponseEntity<DataHome>getInfoHomeDacha(
            @RequestHeader(required = false, value = "Authorization") String token) {

        if (!userService.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        DataHome data = new DataHome();
        data.setTimestamp(System.currentTimeMillis());
        data.setBatterySoc(85.5);
        data.setBatteryStatus("CHARGING");
        data.setBatteryVol(52.3);
        data.setBatteryCurrent(10.2);
        data.setGridStatusRealTime(true);
        data.setSolarPower(3200.0);

        return ResponseEntity.ok(data);
    }


}
