package org.nickas21.smart.data.dataEntityDto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nickas21.smart.usr.data.ErrorCode;
import org.nickas21.smart.usr.data.ErrorLevel;
import org.nickas21.smart.usr.io.UsrTcpWiFiPacketRecordError;

import java.nio.charset.StandardCharsets;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorInfoDto {
    private long timestamp;
    private ErrorCode code;
    private ErrorLevel level;
    private String message;
    private String source; // Додано для ідентифікації порту/джерела

    public ErrorInfoDto(ErrorCode code, String message) {
        this.timestamp = System.currentTimeMillis();
        this.code = code;
        this.level = code.getLevel();
        this.message = message;
    }

    /**
     * Конструктор для перетворення технічної помилки пакета у формат DTO
     */
    public ErrorInfoDto(UsrTcpWiFiPacketRecordError record) {
        this.timestamp = record.timestamp();

        // Спроба мапінгу технічного коду String на Enum ErrorCode
        // Якщо прямого мапінгу немає, можна встановити загальний код помилки зв'язку
        try {
            this.code = ErrorCode.valueOf(record.codeError());
        } catch (IllegalArgumentException | NullPointerException e) {
            this.code = ErrorCode.COMMUNICATION_LOST; // Fallback за замовчуванням
        }

        this.level = (this.code != null) ? this.code.getLevel() : ErrorLevel.WARN;

        // Формуємо повідомлення, включаючи тип пакету та payload (якщо це текст)
        this.message = String.format("Type: %s; Payload: %s",
                record.type(),
                new String(record.payload(), StandardCharsets.UTF_8).trim());

        this.source = "Port: " + record.port();
    }
}