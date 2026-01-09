package org.nickas21.smart.data.dataEntityDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Data
@AllArgsConstructor
public class DataUnitDto {
    List<BatteryInfoDto> batteries;
    InverterDto inverter;
    List<DeviceDto> devices;
}
