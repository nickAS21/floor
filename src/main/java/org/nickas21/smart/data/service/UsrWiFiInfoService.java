package org.nickas21.smart.data.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.data.dataEntityDto.DataUsrWiFiInfoDto;
import org.nickas21.smart.data.dataEntityDto.UsrWiFiInfoVendor;
import org.nickas21.smart.util.JacksonUtil;
import org.nickas21.smart.util.LocationType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.nickas21.smart.util.JacksonUtil.fromString;

@Slf4j
@Service
public class UsrWiFiInfoService {

    @Value("${usr.tcp.infos.dir:./usrInfos/}")
    @Getter
    private String dirUsrInfo;

    @Value("${usr.tcp.infos.fileName:usrInfo.jsonl}")
    @Getter
    private String fileUsrInfo;

    // Кеш у пам'яті для швидкого доступу (Key: id)
    private final Map<Integer, DataUsrWiFiInfoDto> cacheGolego = new ConcurrentHashMap<>();
    private final Map<Integer, DataUsrWiFiInfoDto> cacheDacha = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadFromFile();
    }

    /**
     * Зчитування файлу при старті поду (Kubernetes)
     */
    private void loadFromFile() {
        Path path = Paths.get(dirUsrInfo, fileUsrInfo);
        if (!Files.exists(path)) {
            log.info("Файл даних не знайдено, створюємо новий за шляхом: {}", path.toAbsolutePath());
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                DataUsrWiFiInfoDto usrWiFiInfoDto = fromString(line, DataUsrWiFiInfoDto.class);
                updateInternalCache(usrWiFiInfoDto);
            }
            log.info("Дані успішно завантажені: Golego={}, Dacha={}", cacheGolego.size(), cacheDacha.size());
        } catch (IOException e) {
            log.error("Помилка при читанні файлу конфігурації: ", e);
        }
    }

    private void updateInternalCache(DataUsrWiFiInfoDto dataUsrWiFiInfoDto) {
        if (dataUsrWiFiInfoDto != null) {
        if (dataUsrWiFiInfoDto.getLocationType() == LocationType.GOLEGO) {
            cacheGolego.put(dataUsrWiFiInfoDto.getId(), dataUsrWiFiInfoDto);
        } else {
            cacheDacha.put(dataUsrWiFiInfoDto.getId(), dataUsrWiFiInfoDto);
        }
        }
    }

    public DataUsrWiFiInfoDto getUsrWiFiInfoGolego() {
        // Повертаємо останній актуальний стан для Golego (наприклад, останній доданий)
        return cacheGolego.values().stream().reduce((first, second) -> second).orElse(new DataUsrWiFiInfoDto());
    }

    public DataUsrWiFiInfoDto getUsrWiFiInfoDacha() {
        return cacheDacha.values().stream().reduce((first, second) -> second).orElse(new DataUsrWiFiInfoDto());
    }

    public DataUsrWiFiInfoDto setUsrWiFiInfolego(DataUsrWiFiInfoDto usrWiFiInfoDto) {
        usrWiFiInfoDto.setLocationType(LocationType.GOLEGO);
        return processAndSave(usrWiFiInfoDto);
    }

    public DataUsrWiFiInfoDto setUsrWiFiInfoDacha(DataUsrWiFiInfoDto usrWiFiInfoDto) {
        usrWiFiInfoDto.setLocationType(LocationType.DACHA);
        return processAndSave(usrWiFiInfoDto);
    }

    /**
     * Обчислення Vendor, оновлення кешу та запис у файл
     */
    private synchronized DataUsrWiFiInfoDto processAndSave(DataUsrWiFiInfoDto usrWiFiInfoDto) {
        // 1. Автоматично обчислюємо Vendor за MAC-адресою
        usrWiFiInfoDto.setOui(UsrWiFiInfoVendor.getOuiVendorName(usrWiFiInfoDto.getBssidMac()));

        // 2. Оновлюємо кеш у пам'яті
        updateInternalCache(usrWiFiInfoDto);

        // 3. Дозаписуємо в JSONL файл (Persistence)
        Path path = Paths.get(dirUsrInfo, fileUsrInfo);
        try {
            Files.createDirectories(path.getParent());
            String jsonLine = JacksonUtil.toString(usrWiFiInfoDto) + System.lineSeparator();
            Files.writeString(path, jsonLine, StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
            log.info("Дані записані у файл для ID: {}", usrWiFiInfoDto.getId());
        } catch (IOException e) {
            log.error("Помилка запису у файл: ", e);
        }

        return usrWiFiInfoDto;
    }
}