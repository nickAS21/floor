package org.nickas21.smart.data.controller;

import org.nickas21.smart.data.dataEntityDto.DataUnitDto;
import org.nickas21.smart.data.service.DataUnitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/unit")
public class DataUnitController {

    private final DataUnitService dataUnitService;

    public DataUnitController(DataUnitService dataUnitService) {
        this.dataUnitService = dataUnitService;
    }

    @GetMapping("/golego")
    public ResponseEntity<DataUnitDto> getInfoHomeGolego() {
        return ResponseEntity.ok(this.dataUnitService.getUnitGolego());
    }

//    @GetMapping("/dacha")
//    public ResponseEntity<DataHomeDto> getInfoHomeDacha() {
//        return ResponseEntity.ok(this.dataUnitService.getUnitDacha());
//    }
}
