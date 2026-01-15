package org.nickas21.smart.data.dataEntityDto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nickas21.smart.usr.data.ErrorCode;
import org.nickas21.smart.usr.data.ErrorLevel;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorInfoDto {
    private ErrorCode code;
    private ErrorLevel level; // Буде заповнюватися автоматично на основі code
    private String message;
    private String source;

    public ErrorInfoDto(ErrorCode code, String message, String source) {
        this.code = code;
        this.level = code.getLevel(); // Автоматичне призначення
        this.message = message;
        this.source = source;
    }
}