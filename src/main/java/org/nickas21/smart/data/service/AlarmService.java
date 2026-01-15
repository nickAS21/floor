package org.nickas21.smart.data.service;


import org.nickas21.smart.data.dataEntityDto.ErrorInfoDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AlarmService {


    public List<ErrorInfoDto> getGolegoErrors() {
        List<ErrorInfoDto> errorInfoDtos = new ArrayList<>();
        // read from file GolegoErrors
//        historyGolegoaDtos.add(new  HistoryDto());
        return errorInfoDtos;
    }

    public List<ErrorInfoDto> getDachaErrors() {
        List<ErrorInfoDto> errorInfoDtos = new ArrayList<>();
        // read from file DachaErrors
//        historyGolegoaDtos.add(new  HistoryDto());
        return errorInfoDtos;
    }

    public void clearGolegoErrors() {

    }

    public void clearDachaErrors() {

    }
}



