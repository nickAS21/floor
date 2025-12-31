package org.nickas21.smart.data.service;


import org.nickas21.smart.data.dataEntityDto.HistoryDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class HistoryService {


    public List<HistoryDto> getHistoryGolego() {
        List<HistoryDto> historyGolegoaDtos = new ArrayList<>();
        historyGolegoaDtos.add(new  HistoryDto());
        return historyGolegoaDtos;
    }

    public List<HistoryDto> getHistoryDacha() {
        List<HistoryDto> historyDachaDtos = new ArrayList<>();
        historyDachaDtos.add(new  HistoryDto());
        return historyDachaDtos;
    }
}


