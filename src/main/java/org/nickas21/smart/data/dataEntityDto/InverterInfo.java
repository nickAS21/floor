package org.nickas21.smart.data.dataEntityDto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.nickas21.smart.util.LocationType;

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
            "120A MPPT Charge Controller (NON)",
            48,
            LocationType.GOLEGO
    ),
    DACHA(
            "Hybrid Inverter",
            "Deye",
            "SUN-12K-SG05LP3-EU-SM2 -> master (port: 8900) + slave (port: 8901)",
            24.0,
            LocalDate.of(2026, 5, 4),
            PhaseType.THREE_PHASE,
            true,
            "240А master: 2*MPPT, PV: 1+1; slave: 2*MPPT, PV: 1+1.",
            48,
            LocationType.DACHA
    );

    private final String productName;
    private final String manufacturer;
    @Setter // Додаємо сетер, щоб змінювати значення на льоту
    private String modelName;
    private final double ratedPower;
    private final String commissioningDate;
    private final String phaseType;
    @JsonProperty("isHybrid")
    private final boolean isHybrid;
    private final String mpptControllerName;
    private final int inputVoltage;

    // Нові поля
    private final LocationType location;
    private final String zoneId;

    InverterInfo(String productName, String manufacturer, String modelName, double ratedPower,
                 LocalDate date, PhaseType phase,
                 boolean isHybrid, String mppt, int voltage, LocationType location) {
        this.productName = productName;
        this.manufacturer = manufacturer;
        this.modelName = modelName;
        this.ratedPower = ratedPower;
        this.commissioningDate = formatTimestamp(date.toEpochDay() * 86400000L , datePatternGridStatus);
        this.phaseType = phase.getDescription();
        this.isHybrid = isHybrid;
        this.mpptControllerName = mppt;
        this.inputVoltage = voltage;

        // Встановлюємо локацію та автоматично беремо з неї zoneId
        this.location = location;
        this.zoneId = location.getZoneId().getId();
    }
}

@Getter
enum PhaseType {
    SINGLE_PHASE("One Phase"),
    THREE_PHASE("Three Phase");

    private final String description;
    PhaseType(String description) { this.description = description; }
}