package org.nickas21.smart.data.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/logs")
public class LogsController extends BaseController{

    @GetMapping("/golego")
    public ResponseEntity<String> getLogsGolego(
            @RequestHeader(required = false, value = "Authorization") String token) {

        if (!userService.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok("ok");
    }
    @GetMapping("/dacha")
    public ResponseEntity<String> getLogsDacha(
            @RequestHeader(required = false, value = "Authorization") String token) {

        if (!userService.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok("ok");
    }
}
