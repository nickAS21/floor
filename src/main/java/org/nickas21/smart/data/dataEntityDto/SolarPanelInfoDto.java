package org.nickas21.smart.data.dataEntityDto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SolarPanelInfoDto {
    long timeStamp;
    int pvIndex;
    String parallelInfo;
    double pvVoltageCurV;
    double pvCurrentCurA;
    double pvPowerCurW;

    public String getPvKey() {
        return parallelInfo + "_PV" + pvIndex;
    }
}
