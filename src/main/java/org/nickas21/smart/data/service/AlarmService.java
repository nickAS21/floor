package org.nickas21.smart.data.service;


import org.nickas21.smart.data.dataEntityDto.DataErrorInfoDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AlarmService {


    public List<DataErrorInfoDto> getGolegoErrors() {
        List<DataErrorInfoDto> errorInfoDtos = new ArrayList<>();
        // read from file GolegoErrors
//        historyGolegoaDtos.add(new  DataHistoryDto());
        return errorInfoDtos;
    }

    public List<DataErrorInfoDto> getDachaErrors() {
        List<DataErrorInfoDto> dataErrorInfoDtos = new ArrayList<>();
        // read from file DachaErrors
//        historyGolegoaDtos.add(new  DataHistoryDto());
        return dataErrorInfoDtos;
    }

    public void clearGolegoErrors() {

    }

    public void clearDachaErrors() {

    }
}



