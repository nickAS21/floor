package org.nickas21.smart.usr.entity.dacha;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nickas21.smart.usr.entity.InverterDataBase;
import org.nickas21.smart.util.LocationType;

@EqualsAndHashCode(callSuper = true)
@Data
public class InverterDataDacha extends InverterDataBase {

    private InverterDataDachaOutToHomeBlock8 inverterDataDachaOutToHomeBlock8;
    private InverterDataDachaBmsBlock16 inverterDataDachaBmsBlock16;
    private InverterDataLoadDcBlock80 inverterDataLoadDcBlock80;
    private InverterDataDachaAcBatteryBlock106 inverterDataDachaAcBatteryBlock106;
    private InverterDataDachaDailyTotalBlock118 inverterDataDachaDailyTotalBlock118;

    public InverterDataDacha(int port) {
        super(port, LocationType.DACHA.getZoneId());
    }

}
