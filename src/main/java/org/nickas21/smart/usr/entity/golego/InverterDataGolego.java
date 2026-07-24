package org.nickas21.smart.usr.entity.golego;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nickas21.smart.usr.entity.InverterDataBase;
import org.nickas21.smart.util.LocationType;

@Data
@EqualsAndHashCode(callSuper = true)
public class InverterDataGolego  extends InverterDataBase {

    private InverterGolegoData90 inverterGolegoData90;
    private InverterGolegoData32 inverterGolegoData32;

    public InverterDataGolego(int port) {
        super(port, LocationType.GOLEGO.getZoneId());
    }

    public void inverterDataUpdate (InverterGolegoData90 inverterGolegoData90) {
        updateTime();
        this.inverterGolegoData90 = inverterGolegoData90;
    }

    public void inverterDataUpdate (InverterGolegoData32 inverterGolegoData32) {
        updateTime();
        this.inverterGolegoData32 = inverterGolegoData32;
    }
}
