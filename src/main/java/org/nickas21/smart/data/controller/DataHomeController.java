package org.nickas21.smart.data.controller;

import org.nickas21.smart.data.dataEntityDto.DataHomeDto;
import org.nickas21.smart.data.service.DataHomeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/home")
public class DataHomeController extends BaseController {

    private final DataHomeService dataHomeService;

    public DataHomeController(DataHomeService dataHomeService) {
        this.dataHomeService = dataHomeService;
    }

    @GetMapping("/golego")
    public ResponseEntity<DataHomeDto> getInfoHomeGolego() {
        return ResponseEntity.ok(this.dataHomeService.getDataGolego());
    }

    @GetMapping("/dacha")
    public ResponseEntity<DataHomeDto> getInfoHomeDacha() {
        return ResponseEntity.ok(this.dataHomeService.getDataDacha());
    }
}