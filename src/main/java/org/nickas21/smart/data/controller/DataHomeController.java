package org.nickas21.smart.data.controller;

import org.nickas21.smart.data.dataEntity.DataHome;
import org.nickas21.smart.data.service.DataHomeService;
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
    private final DataHomeService dataHomeService;

//    private final TuyaDeviceService tuyaDeviceService;
//    private final TuyaConnectionProperties tuyaConnectionProperties;

//    public DataHomeController(UserService userService, TuyaDeviceService tuyaDeviceService, TuyaConnectionProperties tuyaConnectionProperties) {
    public DataHomeController(UserService userService, DataHomeService dataHomeService) {
        this.userService = userService;
        this.dataHomeService = dataHomeService;
    }


    @GetMapping("/golego")
    public ResponseEntity<DataHome> getInfoHomeGolego(
            @RequestHeader(required = false, value = "Authorization") String token) {

        if (!userService.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(this.dataHomeService.getDataGolego());
    }

    @GetMapping("/dacha")
    public ResponseEntity<DataHome>getInfoHomeDacha(
            @RequestHeader(required = false, value = "Authorization") String token) {

        if (!userService.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(this.dataHomeService.getDataDacha());
    }


}
