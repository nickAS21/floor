package org.nickas21.smart.solarman;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.solarman.api.Communication;
import org.nickas21.smart.solarman.api.HistoricalOneDayTimeData;
import org.nickas21.smart.solarman.api.RealTimeData;
import org.nickas21.smart.solarman.api.SolarmanToken;
import org.nickas21.smart.solarman.api.Station;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

        this.webClient = WebClient.builder()
                .baseUrl(solarmanConnectionProperties.getRegion().getApiUrl())
                .filter((request, next) -> {
                    // Отримуємо токен безпечно
                    SolarmanToken token = getSolarmanToken(1);

                    // Якщо токен є і він валідний - додаємо авторизацію
                    if (token != null && token.getAccessToken() != null) {
                        ClientRequest authenticatedRequest = ClientRequest.from(request)
                                .headers(h -> h.setBearerAuth(token.getAccessToken()))
                                .build();
                        return next.exchange(authenticatedRequest);
                    }

                    // Якщо токена немає - працюємо "локально" (запит піде без токена або можна викинути свій error)
                    log.warn("Working in local mode: Solarman token missing");
                    return next.exchange(request);
                })
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
        if (stations.isEmpty()) {
            log.error("Bad start. Solarman stations required, none available.");
            System.exit(0);
        }
    }


    private SolarmanToken getSolarmanToken(int cntCreteToken) {
        try {
            // Check if we need to fetch a new token
            if (this.accessSolarmanToken == null || !hasValidAccessToken()) {
                if (this.accessSolarmanToken != null) {
                    log.info("Token expired or invalid. Refreshing...");
                }
                this.accessSolarmanToken = createSolarmanToken();
            }
        } catch (Exception e) {
            if (cntCreteToken > 0) {
                log.warn("Token creation failed. Retrying... Attempts left: {}", cntCreteToken);
                // Decrease counter and retry
                return getSolarmanToken(cntCreteToken - 1);
            } else {
                // Critical: Authorization failed after all retries
                log.error("Failed to acquire Solarman token: {}. Operating in offline/local mode.", e.getMessage());

                // Return an empty object instead of null to prevent NPE in downstream filters
                if (this.accessSolarmanToken == null) {
                    this.accessSolarmanToken = new SolarmanToken();
                }
            }
        }
        return this.accessSolarmanToken;
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
        if (!stations.isEmpty()) {
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

    private void getStationList() {
        // Initialize to prevent NullPointerException elsewhere
        this.stations = new ConcurrentHashMap<>();

        try {
            // Use blockOptional to handle empty responses gracefully
            Optional<StationListResponse> response = webClient.post()
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
                    .onStatus(HttpStatusCode::isError, clientResponse -> {
                        log.error("Solarman API error status: {}", clientResponse.statusCode());
                        return Mono.empty();
                    })
                    .bodyToMono(StationListResponse.class)
                    .timeout(Duration.ofSeconds(10))
                    .blockOptional();

            // Safely process the list only if the response exists
            response.ifPresent(res -> {
                if (res.stationList() != null) {
                    res.stationList().forEach(s -> stations.put(s.getId(), s));
                    log.info("Successfully loaded {} stations from Solarman", stations.size());
                }
            });

        } catch (Exception e) {
            // If password is wrong or internet is down, we land here.
            // The application continues running in 'local mode'.
            log.error("Offline mode: Could not fetch Solarman stations. Reason: {}", e.getMessage());
        }
    }

    private void getDeviceCommunication(String loggerSn) {
        try {
            // Твій запит, але з таймаутом, щоб не чекати вічно
            Optional<CommunicationResponse> result = webClient.post()
                    .uri(uriBuilder -> uriBuilder
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
                    .timeout(Duration.ofSeconds(10))
                    .blockOptional();

            // Замість orElseThrow() використовуємо ifPresent, щоб не крашити додаток
            result.ifPresent(response -> {
                Communication communication = response.communication();
                if (communication == null) return;

                // Твоя перевірка ChildList, але безпечна (без IndexOutOfBoundsException)
                if (communication.getChildList() != null && !communication.getChildList().isEmpty()) {
                    var firstInverter = communication.getChildList().getFirst();
                    boolean isError = false;

                    // Перевірка Inverter SN
                    if (StringUtils.hasLength(firstInverter.getDeviceSn())) {
                        solarmanStation.setInverterSn(firstInverter.getDeviceSn());
                    } else {
                        log.error("Create solarman InverterInfo Sn required, not null.");
                        isError = true;
                    }

                    // Перевірка Inverter ID
                    if (firstInverter.getDeviceId() != null && firstInverter.getDeviceId() > 0) {
                        solarmanStation.setInverterId(firstInverter.getDeviceId());
                    } else {
                        log.error("Create solarman InverterInfo Id required, not zero.");
                        isError = true;
                    }

                    // Перевірка Logger ID
                    if (communication.getDeviceId() != null && communication.getDeviceId() > 0) {
                        solarmanStation.setLoggerId(communication.getDeviceId());
                    } else {
                        log.error("Create solarman Logger Id required, not zero.");
                        isError = true;
                    }

                    if (!isError) {
                        log.info("InverterSn: [{}], InverterId: [{}], LoggerId: [{}]",
                                solarmanStation.getInverterSn(),
                                solarmanStation.getInverterId(),
                                solarmanStation.getLoggerId());
                    }
                } else {
                    log.warn("ChildList is empty for logger: {}", loggerSn);
                }
            });

        } catch (Exception e) {
            // Якщо все впало (немає нету, auth failed), ми просто йдемо далі
            log.error("Solarman communication failed for {}: {}. Offline mode active.", loggerSn, e.getMessage());
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
        try {
            // 1. Validate if we have the necessary IDs before calling the API
            if (solarmanStation.getInverterId() == null || solarmanStation.getInverterId() <= 0) {
                log.warn("Cannot fetch real-time data: Inverter ID is missing or invalid.");
                return null; // Or return a default/empty RealTimeData object
            }

            // 2. Execute request with safety measures
            Optional<RealTimeData> response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(DEVICE_CURRENT_DATA)
                            .queryParam("language", "en")
                            .build())
                    .headers(httpHeaders -> {
                        httpHeaders.add("t", String.valueOf(System.currentTimeMillis()));
                        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                    })
                    .bodyValue(new RealTimeDataRequest(solarmanStation.getInverterSn(), solarmanStation.getInverterId()))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> {
                        log.error("Solarman RealTimeData API error: {}", clientResponse.statusCode());
                        return Mono.empty();
                    })
                    .bodyToMono(RealTimeData.class)
                    .timeout(Duration.ofSeconds(15)) // Prevent hanging forever
                    .blockOptional();

            // 3. Return data or log the absence of it
            if (response.isEmpty()) {
                log.warn("No real-time data received from Solarman. Check connectivity or credentials.");
            }

            return response.orElse(null); // Return null instead of throwing an exception

        } catch (Exception e) {
            // Catch network issues, timeouts, and logic errors
            log.error("Failed to fetch real-time data: {}. Continuing in local mode.", e.getMessage());
            return null;
        }
    }

    public HistoricalOneDayTimeData fetchHistoricalOneDayTimeData(Instant instant) {
        // 1. Format date correctly (e.g., "2026-05-07")
        String dateStr = instant.atZone(ZoneId.systemDefault()).toLocalDate().toString();

        try {
            // 2. Pre-check: Do we have the Inverter ID?
            if (solarmanStation.getInverterId() == null || solarmanStation.getInverterId() <= 0) {
                log.warn("Skipping historical data fetch: Inverter ID is missing for date: {}", dateStr);
                return null;
            }

            log.info("Requesting Solarman historical data for date: {}", dateStr);

            // 3. Safe WebClient call
            Optional<HistoricalOneDayTimeData> response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
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
                            dateStr,
                            dateStr,
                            2  // Time interval (e.g., 10 minutes)
                    ))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> {
                        log.error("Solarman History API error status: {}", clientResponse.statusCode());
                        return Mono.empty();
                    })
                    .bodyToMono(HistoricalOneDayTimeData.class)
                    .timeout(Duration.ofSeconds(20)) // Historical data can take longer to process
                    .blockOptional();

            if (response.isEmpty()) {
                log.warn("No historical data found for date: {}", dateStr);
            }

            return response.orElse(null);

        } catch (Exception e) {
            // Handle network issues or timeouts without crashing
            log.error("Failed to fetch historical data for {}: {}. Continuing locally.", dateStr, e.getMessage());
            return null;
        }
    }

    private boolean hasValidAccessToken() {
        // 1. Permanent safety check for the token object and the actual string
        if (accessSolarmanToken == null || accessSolarmanToken.getAccessToken() == null) {
            return false;
        }

        // 2. Safe retrieval of the Long object
        Long expiresIn = accessSolarmanToken.getExpiresIn();

        // 3. Null-safe comparison to prevent unboxing error in Java 25
        if (expiresIn == null) {
            log.warn("Token expires_in field is null. Considering token invalid.");
            return false;
        }

        // 4. Time synchronization check (converting to milliseconds if needed)
        // Assuming expiresIn is in seconds from the API response
        long expiryTimeMillis = expiresIn * 1000L;
        long bufferMillis = 20_000L; // 20 seconds buffer

        return (expiryTimeMillis - bufferMillis) > System.currentTimeMillis();
    }

    private record SolarmanAuthRequest(String appSecret, String email, String password) { }

    private record StationListRequest(int page, int size) { }

    private record StationListResponse(List<Station> stationList) { }

    private record CommunicationRequest(String deviceSn) { }

    private record CommunicationResponse(Communication communication) { }

    private record RealTimeDataRequest(String deviceSn, Long deviceId) { }

    private record HistoricalDataRequest(Long deviceId,String deviceSn, String startTime, String endTime, Integer timeType) { }
}

