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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class UsrWiFiInfoService {

    @Value("${usr.tcp.infos.dir:./usrInfos/}")
    @Getter
    private String dirUsrInfo;

    @Value("${usr.tcp.infos.fileName:usrInfo.json}")
    @Getter
    private String fileUsrInfo;

    private final Map<Integer, DataUsrWiFiInfoDto> cacheGolego = new ConcurrentHashMap<>();
    private final Map<Integer, DataUsrWiFiInfoDto> cacheDacha = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadFromFile();
    }

    private void loadFromFile() {
        Path path = Paths.get(dirUsrInfo, fileUsrInfo);
        if (!Files.exists(path)) {
            log.info("Файл не знайдено: {}", path.toAbsolutePath());
            return;
        }

        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            if (json == null || json.isBlank()) return;

            // Використовуємо новий метод для десеріалізації списку
            List<DataUsrWiFiInfoDto> allData = JacksonUtil.fromStringToList(json, DataUsrWiFiInfoDto.class);

            if (allData != null) {
                allData.forEach(this::updateInternalCache);
                log.info("Кеш ініціалізовано: Golego={}, Dacha={}", cacheGolego.size(), cacheDacha.size());
            }
        } catch (IOException e) {
            log.error("Помилка завантаження кешу з JSON: ", e);
        }
    }

    private void updateInternalCache(DataUsrWiFiInfoDto dto) {
        if (dto == null) return;
        if (dto.getLocationType() == LocationType.GOLEGO) {
            cacheGolego.put(dto.getId(), dto);
        } else {
            cacheDacha.put(dto.getId(), dto);
        }
    }

    public List<DataUsrWiFiInfoDto> getUsrWiFiInfoGolego() {
        return new ArrayList<>(cacheGolego.values());
    }

    public List<DataUsrWiFiInfoDto> getUsrWiFiInfoDacha() {
        return new ArrayList<>(cacheDacha.values());
    }

    public List<DataUsrWiFiInfoDto> setUsrWiFiInfolego(List<DataUsrWiFiInfoDto> dtoList) {
        return processAndSaveAll(dtoList, LocationType.GOLEGO);
    }

    public List<DataUsrWiFiInfoDto> setUsrWiFiInfoDacha(List<DataUsrWiFiInfoDto> dtoList) {
        return processAndSaveAll(dtoList, LocationType.DACHA);
    }

    private synchronized List<DataUsrWiFiInfoDto> processAndSaveAll(List<DataUsrWiFiInfoDto> dtoList, LocationType type) {
        // Очищаємо кеш локації, бо фронт прислав ПОВНИЙ актуальний список
        if (type == LocationType.GOLEGO) cacheGolego.clear();
        else cacheDacha.clear();

        for (DataUsrWiFiInfoDto dto : dtoList) {
            dto.setLocationType(type);
            dto.setOui(UsrWiFiInfoVendor.getOuiVendorName(dto.getBssidMac()));
            updateInternalCache(dto);
        }

        saveAllToFile();
        return (type == LocationType.GOLEGO) ? getUsrWiFiInfoGolego() : getUsrWiFiInfoDacha();
    }

    private synchronized void saveAllToFile() {
        Path path = Paths.get(dirUsrInfo, fileUsrInfo);
        try {
            Files.createDirectories(path.getParent());

            List<DataUsrWiFiInfoDto> allItems = new ArrayList<>();
            allItems.addAll(cacheGolego.values());
            allItems.addAll(cacheDacha.values());

            String json = JacksonUtil.toString(allItems);

            Files.writeString(path, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            log.info("Файл оновлено. Загальна кількість записів: {}", allItems.size());
        } catch (IOException e) {
            log.error("Помилка запису у файл: ", e);
        }
    }
}