package org.nickas21.smart.data.service;

import lombok.RequiredArgsConstructor;
import org.nickas21.smart.data.service.logsDacha.RuntimeLogBuffer;
import org.nickas21.smart.tuya.TuyaDeviceService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogsService {

    private final TuyaDeviceService deviceService;
    private final RuntimeLogBuffer buffer;


    public String getLogsDacha() {
        int limit = this.deviceService.getLogsDachaLimit();
        return buffer.get(limit);
    }

//    public BatteryCellInfo getLogsGolego() throws IOException {
//        // Твоя логіка для Golego
//        return new BatteryCellInfo();
//    }
}