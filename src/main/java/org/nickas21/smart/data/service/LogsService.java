package org.nickas21.smart.data.service;

import lombok.RequiredArgsConstructor;
import org.nickas21.smart.data.service.systemOut.RuntimeLogBuffer;
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

    public String getLogsAppCurrent() {
        return buffer.getCurrentLogs();
    }

    public String getLogsAppPrevious() {
        return buffer.getPreviousLogs();
    }
}