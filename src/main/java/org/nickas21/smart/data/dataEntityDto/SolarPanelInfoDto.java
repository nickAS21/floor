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
    Long timeStamp;
    Integer pvIndex;
    String parallelInfo;

    Double pvVoltageCurV;
    Double pvCurrentCurA;
    Double pvPowerCurW;

    String vendor;
    String modelName;
    Integer panelsCount;

    public String getPvKey() {
        return parallelInfo + "_PV" + pvIndex;
    }
}
