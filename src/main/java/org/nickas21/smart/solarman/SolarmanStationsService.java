package org.nickas21.smart.solarman;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.solarman.api.Communication;
import org.nickas21.smart.solarman.api.HistoricalOneDayTimeData;
import org.nickas21.smart.solarman.api.RealTimeData;
import org.nickas21.smart.solarman.api.SolarmanToken;
import org.nickas21.smart.solarman.api.Station;
import org.nickas21.smart.util.HttpUtil;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static org.nickas21.smart.solarman.api.ApiPath.DEVICE_COMMUNICATION;
import static org.nickas21.smart.solarman.api.ApiPath.DEVICE_CURRENT_DATA;
import static org.nickas21.smart.solarman.api.ApiPath.DEVICE_HISTORICAL_DATA;
import static org.nickas21.smart.solarman.api.ApiPath.STATION_LIST;
import static org.nickas21.smart.solarman.api.ApiPath.TOKEN;
import static org.nickas21.smart.util.JacksonUtil.fromString;

@Slf4j
@Service
@EnableConfigurationProperties({SolarmanConnectionProperties.class, SolarmanStationProperties.class})
public class SolarmanStationsService {
    private final SolarmanConnectionProperties solarmanConnectionProperties;
    private final SolarmanStationProperties stationProperties;
    @Getter
    private SolarmanStation solarmanStation;
    private SolarmanToken accessSolarmanToken;
    private Map<Long, Station> stations;
    private final WebClient authClient = WebClient.builder().build();
    private final WebClient webClient;

    public SolarmanStationsService(SolarmanConnectionProperties solarmanConnectionProperties, SolarmanStationProperties stationProperties) {
        this.solarmanConnectionProperties = solarmanConnectionProperties;
        this.stationProperties = stationProperties;
        int cntCreteToken = 3;
        webClient = WebClient.builder()
                .baseUrl(solarmanConnectionProperties.getRegion().getApiUrl())
                .filter(ExchangeFilterFunction.ofRequestProcessor(
                        (ClientRequest request) -> Mono.just(ClientRequest.from(request)
                                .headers(httpHeaders -> httpHeaders.setBearerAuth(getSolarmanToken(cntCreteToken).getAccessToken()))
                                .build())))
                .build();
    }

    public void init() {
        solarmanStation = SolarmanStation.builder()
                .timeoutSec(stationProperties.getTimeoutSec())
                .batSocMinMin(stationProperties.getBatSocMinMin())
                .batSocMinMax(stationProperties.getBatSocMinMax())
                .batSocMax(stationProperties.getBatSocMax())
                .batSocAlarmWarn(stationProperties.getBatSocAlarmWarn())
                .batSocAlarmError(stationProperties.getBatSocAlarmError())
                .stationConsumptionPower(stationProperties.getStationConsumptionPower())
                .dopPowerToMax(stationProperties.getDopPowerToMax())
                .dopPowerToMin(stationProperties.getDopPowerToMin())
                .seasonsId(stationProperties.getSeasonsId())
                .build();
        initAfterTokenSuccess();
        if (stations.size() == 0) {
            log.error("Bad start. Solarman stations required, none available.");
            System.exit(0);
        }
    }


    private SolarmanToken getSolarmanToken(int cntCreteToken) {
        try {
            if (accessSolarmanToken != null) {
                if (!hasValidAccessToken()) {
                    log.info("ReCreate Solarman token: expireIn [{}] currentDate [{}]", Instant.ofEpochMilli(this.accessSolarmanToken.getExpiresIn() + 20_000).toString(),
                            HttpUtil.toLocaleTimeString(Instant.now()));
                    accessSolarmanToken = createSolarmanToken();
                }
            } else {
                accessSolarmanToken = createSolarmanToken();
            }
        } catch (Exception e){
            if (cntCreteToken > 0) {
                cntCreteToken--;
                getSolarmanToken(cntCreteToken);
            } else {
                log.error("Failed Solarman token: [{}]", e.getMessage());
                System.exit(0);
            }
        }
        return accessSolarmanToken;
    }

    private SolarmanToken createSolarmanToken() {
        var body = new SolarmanAuthRequest(solarmanConnectionProperties.getSecret(),
                solarmanConnectionProperties.getUsername(),
                solarmanConnectionProperties.getPasswordHash()
        );

        var token = authClient.post()
                .uri(solarmanConnectionProperties.getRegion().getApiUrl(), uriBuilder -> uriBuilder
                        .path(TOKEN)
                        .queryParam("appId", solarmanConnectionProperties.getAppid())
                        .queryParam("language", "en")
                        .build())
                .headers(httpHeaders -> {
                    httpHeaders.add("t", String.valueOf(System.currentTimeMillis()));
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                })
                .bodyValue(body)  // <-- body тут використовується
                .exchangeToMono(response ->
                        response.bodyToMono(String.class) // читаємо raw JSON
//                                .doOnNext(rawBody -> System.out.println("Solarman raw response: " + rawBody))
                                .map(rawBody -> {
                                    try {
                                        return Objects.requireNonNull(fromString(rawBody, SolarmanToken.class));
                                    } catch (Exception e) {
                                        throw new RuntimeException("JSON parsing failed", e);
                                    }
                                })
                )
                .blockOptional();

        return token.map(t -> {
            t.setExpiresIn(System.currentTimeMillis() + (t.getExpiresIn() * 1000));
            return t;
        }).orElseThrow();
    }

    private void initAfterTokenSuccess() {
        getStationList();
        if (stations.size() > 0) {
            Long stationId = Long.valueOf(stations.keySet().toArray()[0].toString());
            String stationName = stations.get(stationId).getName();
            solarmanStation.setStationId(stationId);
            solarmanStation.setName(stationName);
            solarmanStation.setLocationLat(stations.get(stationId).getLocationLat());
            solarmanStation.setLocationLng(stations.get(stationId).getLocationLng());
            log.info("First station id: [{}], name [{}]", stationId, stationName);
            String loggerSn = this.solarmanConnectionProperties.getLoggerSn();
            getDeviceCommunication(loggerSn);
        }
    }

    @SneakyThrows
    private void getStationList() {
        stations = new ConcurrentHashMap<>();
        var stationResponse = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(STATION_LIST)
                        .queryParam("language", "en")
                        .build())
                .headers(httpHeaders -> {
                    httpHeaders.add("t", String.valueOf(System.currentTimeMillis()));
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                })
                .bodyValue(new StationListRequest(1, 10))
                .retrieve()
                .bodyToMono(StationListResponse.class)
                .blockOptional();

        stationResponse.orElseThrow().stationList().forEach(station -> stations.put(station.getId(), station));
    }

    @SneakyThrows
    private void getDeviceCommunication(String loggerSn) {
        Map<String, Communication> communications = new ConcurrentHashMap<>();
        var result = webClient.post().uri(uriBuilder -> uriBuilder
                .path(DEVICE_COMMUNICATION)
                .queryParam("language", "en")
                .build())
                .headers(httpHeaders -> {
                    httpHeaders.add("t", String.valueOf(System.currentTimeMillis()));
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                })
                .bodyValue(new CommunicationRequest(loggerSn))
                .retrieve()
                .bodyToMono(CommunicationResponse.class)
                .blockOptional();

        var communication = result.orElseThrow().communication();

        communications.put(solarmanConnectionProperties.getLoggerSn(), communication);
        boolean isLoggerInverterError = false;
        if (StringUtils.hasLength(communications.get(loggerSn).getChildList().get(0).getDeviceSn())) {
            solarmanStation.setInverterSn(communications.get(loggerSn).getChildList().get(0).getDeviceSn());
        } else {
            isLoggerInverterError = true;
            log.error("Create solarman InverterInfo Sn required, not null.");
        }
        if (communications.get(loggerSn).getChildList().get(0).getDeviceId() > 0) {
            solarmanStation.setInverterId(communications.get(loggerSn).getChildList().get(0).getDeviceId());
        } else {
            isLoggerInverterError = true;
            log.error("Create solarman InverterInfo Id required, not zero.");
        }
        if (communications.get(loggerSn).getDeviceId() > 0) {
            solarmanStation.setLoggerId(communications.get(loggerSn).getDeviceId());
        } else {
            isLoggerInverterError = true;
            log.error("Create solarman Logger Id required, not zero.");
        }
        if (!isLoggerInverterError) {
            log.info("InverterSn: [{}],  InverterId: [{}],  LoggerId: [{}]",
                    solarmanStation.getInverterSn(), solarmanStation.getInverterId(),
                    solarmanStation.getLoggerId());
        }
    }

    public RealTimeData getRealTimeData() {
        RealTimeData result = fetchRealTimeData();
        if (result.getDataList() == null) {
            result = fetchRealTimeData();
            if (result.getDataList() == null) {
                throw new IllegalStateException(
                        "Solarman returned dataList is null after retrying the request"
                );
            }
        }
        return result;
    }

    public RealTimeData fetchRealTimeData() {
        return webClient.post().uri(uriBuilder -> uriBuilder
                        .path(DEVICE_CURRENT_DATA)
                        .queryParam("language", "en")
                        .build())
                .headers(httpHeaders -> {
                    httpHeaders.add("t", String.valueOf(System.currentTimeMillis()));
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                })
                .bodyValue(new RealTimeDataRequest(solarmanStation.getInverterSn(), solarmanStation.getInverterId()))
                .retrieve()
                .bodyToMono(RealTimeData.class)
                .blockOptional()
                .orElseThrow(() -> new IllegalStateException("Solarman returned null response"));
    }

    public HistoricalOneDayTimeData fetchHistoricalOneDayTimeData(Instant instant) {
        // Форматуємо дату (наприклад, "2026-02-26")
        String dateStr = instant.atZone(ZoneId.systemDefault()).toLocalDate().toString();

        log.info("Запит історії Solarman за дату: {}", dateStr);
        return webClient.post().uri(uriBuilder -> uriBuilder
                        .path(DEVICE_HISTORICAL_DATA)
                        .queryParam("language", "en")
                        .build())
                .headers(httpHeaders -> {
                    httpHeaders.add("t", String.valueOf(System.currentTimeMillis()));
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                })
                .bodyValue(new HistoricalDataRequest(
                        solarmanStation.getInverterId(),
                        solarmanStation.getInverterSn(),
                        dateStr, // Початок діапазону
                        dateStr, // Кінець діапазону (той самий день)
                        2  // Отримуємо точки кожні 10 хвилин
                ))
                .retrieve()
                .bodyToMono(HistoricalOneDayTimeData.class)
                .blockOptional()
                .orElseThrow(() -> new IllegalStateException("Solarman returned null response history"));
    }

    private boolean hasValidAccessToken() {
        return accessSolarmanToken.getExpiresIn() + 20_000 > System.currentTimeMillis();
    }

    private record SolarmanAuthRequest(String appSecret, String email, String password) { }

    private record StationListRequest(int page, int size) { }

    private record StationListResponse(List<Station> stationList) { }

    private record CommunicationRequest(String deviceSn) { }

    private record CommunicationResponse(Communication communication) { }

    private record RealTimeDataRequest(String deviceSn, Long deviceId) { }

    private record HistoricalDataRequest(Long deviceId,String deviceSn, String startTime, String endTime, Integer timeType) { }
}

