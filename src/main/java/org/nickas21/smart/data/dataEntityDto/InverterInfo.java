package org.nickas21.smart.data.dataEntityDto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.LocalDate;

import static org.nickas21.smart.data.dataEntityDto.DataHomeDto.datePatternGridStatus;
import static org.nickas21.smart.util.StringUtils.formatTimestamp;

@Getter
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum InverterInfo {
    GOLEGO(
            "Dual Output Solar Inverter",
            "GOOTU",
            "JSY-H4862E120-D",
            6.2,
            LocalDate.of(2024, 8, 7),
            PhaseType.SINGLE_PHASE,
            true,
            "120A MPPT Charge Controller",
            48
    ),
    DACHA(
            "Hybrid Inverter",
            "Deye",
            "SUN-12K-SG04LP3",
            12.0,
            LocalDate.of(2023, 2, 1),
            PhaseType.THREE_PHASE,
            true,
            "240А 2*MPPT: PV: 2+1",
            48
    );

    private final String productName;
    private final String manufacturer;
    private final String modelName;
    private final double ratedPower;
    private final String commissioningDate; // Long Timestamp
    private final String phaseType;        // Текстовий опис
    @JsonProperty("isHybrid")
    private final boolean isHybrid;
    private final String mpptControllerName;
    private final int inputVoltage;

    InverterInfo(String productName, String manufacturer, String modelName, double ratedPower,
                 LocalDate date, PhaseType phase,
                 boolean isHybrid, String mppt, int voltage) {
        this.productName = productName;
        this.manufacturer = manufacturer;
        this.modelName = modelName;
        this.ratedPower = ratedPower;
        this.commissioningDate = formatTimestamp(date.toEpochDay() * 86400000L , datePatternGridStatus);
        this.phaseType = phase.getDescription();
        this.isHybrid = isHybrid;
        this.mpptControllerName = mppt;
        this.inputVoltage = voltage;
    }
}

@Getter
enum PhaseType {
    SINGLE_PHASE("One Phase"),
    THREE_PHASE("Three Phase");

    private final String description;
    PhaseType(String description) { this.description = description; }
}