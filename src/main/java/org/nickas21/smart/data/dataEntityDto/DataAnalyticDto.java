package org.nickas21.smart.data.dataEntityDto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.util.LocationType;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataAnalyticDto {
    private long timestamp;
    private PowerType powerType;
    private LocationType location;
    private double powerDay;
    private double powerNight;
    private double powerTotal;

    public DataAnalyticDto (LocationType location, PowerType powerType) {
        this.timestamp = System.currentTimeMillis();
        this.location = location;
        this.powerType = powerType;
        // Ініціалізуємо нулями, щоб уникнути Null при математичних операціях
        this.powerDay = 0.0;
        this.powerNight = 0.0;
        this.powerTotal = 0.0;
    }

    public static List<DataAnalyticDto> fromApiList(List<DataAnalyticApiDto> apiList) {
        return apiList.stream().map(apiDto -> {
            DataAnalyticDto dto = new DataAnalyticDto();

            // Копіюємо прості типи
            dto.setTimestamp(apiDto.getTimestamp());
            dto.setPowerDay(apiDto.getPowerDay());
            dto.setPowerNight(apiDto.getPowerNight());
            dto.setPowerTotal(apiDto.getPowerTotal());

            // Безпечно перетворюємо рядки в Енуми
            try {
                if (apiDto.getLocation() != null && !apiDto.getLocation().isEmpty()) {
                    // toUpperCase() лікує проблему маленьких літер з фронта
                    dto.setLocation(LocationType.valueOf(apiDto.getLocation().toUpperCase().trim()));
                }
                if (apiDto.getPowerType() != null && !apiDto.getPowerType().isEmpty()) {
                    dto.setPowerType(PowerType.valueOf(apiDto.getPowerType().toUpperCase().trim()));
                }
            } catch (IllegalArgumentException e) {
                // Якщо прилетів невалідний текст, Енум залишиться null, але сервер не видасть 400
                System.err.println("Error converting Enum: " + e.getMessage());
            }

            return dto;
        }).collect(Collectors.toList());
    }
}

