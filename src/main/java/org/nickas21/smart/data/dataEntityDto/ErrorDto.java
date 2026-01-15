package org.nickas21.smart.data.dataEntityDto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorDto {
    private long timestamp;
    List<ErrorInfoDto> errorInfos = new ArrayList<>();
}

