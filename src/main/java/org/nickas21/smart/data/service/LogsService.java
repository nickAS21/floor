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


    public String getLogsApp() {
        int limit = this.deviceService.getLogsAppLimit();
        return buffer.get(limit);
    }
}