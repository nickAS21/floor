package org.nickas21.smart.usr.entity.golego;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nickas21.smart.usr.entity.InverterDataBase;

import java.util.Objects;

@Data
@EqualsAndHashCode(callSuper = true)
public class InverterDataGolego  extends InverterDataBase {

    private InverterGolegoData90 inverterGolegoData90;
    private InverterGolegoData32 inverterGolegoData32;

    public InverterDataGolego(int port, InverterGolegoData32 inverterGolegoData32, InverterGolegoData90 inverterGolegoData90) {
        super(port);
        this.inverterGolegoData32 = Objects.requireNonNull(inverterGolegoData32, "Data32 cannot be null");
        this.inverterGolegoData90 = Objects.requireNonNull(inverterGolegoData90, "Data90 cannot be null");
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
