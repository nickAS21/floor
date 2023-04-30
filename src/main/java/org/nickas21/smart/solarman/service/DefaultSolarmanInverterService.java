package org.nickas21.smart.solarman.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.solarman.mq.SolarmanToken;
import org.nickas21.smart.solarman.mq.Station;
import org.nickas21.smart.solarman.source.SolarmanMqttDataSource;
import org.nickas21.smart.tuya.mq.TuyaToken;
import org.nickas21.smart.util.JacksonUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.nickas21.smart.solarman.constant.SolarmanApi.GET_SOLARMAN_OBTAIN_STATION_LIST_PATH;
import static org.nickas21.smart.solarman.constant.SolarmanApi.GET_SOLARMAN_TOKEN_URL_PATH;
import static org.nickas21.smart.tuya.constant.TuyaApi.GET_TUYA_REFRESH_TOKEN_URL_PATH;
import static org.nickas21.smart.util.HttpUtil.creatHttpPathWithQueries;
import static org.nickas21.smart.util.HttpUtil.getBodyHash;
import static org.nickas21.smart.util.HttpUtil.sendRequest;
import static org.nickas21.smart.util.JacksonUtil.objectToJsonNode;
import static org.nickas21.smart.util.JacksonUtil.treeToValue;

@Slf4j
@Service
public class DefaultSolarmanInverterService implements SolarmanInverterService {
    private ExecutorService executor;
    private SolarmanMqttDataSource solarmanMqttDataConnection;
    private SolarmanToken accessSolarmanToken;
    private Map<Long, Station> stations;

    @Override
    public void setExecutorService(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public void setSolarmanMqttDataSource(SolarmanMqttDataSource solarmanMqttDataConnection) {
        this.solarmanMqttDataConnection = solarmanMqttDataConnection;
    }

    @Override
    public void init() {
        accessSolarmanToken = getSolarmanToken();
        if (accessSolarmanToken != null) {
            stations = new ConcurrentHashMap<>();
            getStaionList();
        }
    }

    private SolarmanToken getSolarmanToken() {
        if (accessSolarmanToken != null) {
//            if (!hasValidAccessToken()) {
//                accessSolarmanToken = refreshSolarmanToken();
//            }
        } else {
            accessSolarmanToken  = createSolarmanToken();
        }
        return accessSolarmanToken ;
    }
//
//    private boolean hasValidAccessToken() {
//        return accessSolarmanToken.getExpireAt() + 20_000 > System.currentTimeMillis();
//    }

    @SneakyThrows
    private SolarmanToken createSolarmanToken() {
        Future<SolarmanToken> future = executor.submit(() -> {
            try {
                return createGetSolarmanToken();
            } catch (Exception e) {
                log.error("Create solarman token error", e);
                return null;
            } finally {
            }
        });
        SolarmanToken createToken = future.get();
        if (Objects.isNull(createToken)) {
            log.error("Create solarman token required, not null.");
        }
        return createToken;
    }

    @SneakyThrows
    private void getStaionList() {
        Future<JsonNode> future = executor.submit(() -> {
            try {
                return createGetStationList();
            } catch (Exception e) {
                log.error("Create solarman token error", e);
                return null;
            } finally {
            }
        });
        JsonNode result = future.get();
        if (Objects.isNull(result) || result.get("stationList") == null) {
            log.error("Create solarman station list required, not null.");
        } else {
            ArrayNode staionList = (ArrayNode) result.get("stationList");
            for (JsonNode stationNode : staionList) {
                Station station = treeToValue(stationNode, Station.class);
                stations.put(station.getId(), station);
            }

        }

    }

    @SneakyThrows
    private SolarmanToken refreshSolarmanToken() {
        Future<SolarmanToken> future = executor.submit(() -> {
            try {
                return refreshGetSolarmanToken();
            } catch (Exception e) {
                log.error("refresh token error", e);
                return null;
            } finally {
            }
        });
        SolarmanToken refreshedToken = future.get();
        if (Objects.isNull(refreshedToken)) {
            log.error("Refreshed token required, not null.");
        }
        return refreshedToken;
    }


    @SneakyThrows
    private SolarmanToken createGetSolarmanToken() {
        Map<String, Object> queries = new HashMap<>();
        queries.put("appId", this.solarmanMqttDataConnection.getAppId());
        queries.put("language", "en");
        String path = creatHttpPathWithQueries(GET_SOLARMAN_TOKEN_URL_PATH, queries);
        ObjectNode data = JacksonUtil.newObjectNode();
        data.set("appSecret", objectToJsonNode(this.solarmanMqttDataConnection.getSecret()));
        data.set("email", objectToJsonNode(this.solarmanMqttDataConnection.getUserName()));
        String passHash = this.solarmanMqttDataConnection.getPassHash();
        data.set("password", objectToJsonNode(passHash));
        RequestEntity<Object> requestEntity = createRequestWithBody(path, data, HttpMethod.POST, false);
        ResponseEntity<ObjectNode> responseEntity = sendRequest(requestEntity);
        if (responseEntity != null && responseEntity.getBody().get("success").asBoolean()) {
            JsonNode result = responseEntity.getBody();
            return SolarmanToken.builder()
                    .accessToken(result.get("access_token").asText())
                    .refreshToken(result.get("refresh_token").asText())
                    .expiresIn(result.get("expires_in").asText())
                    .uid(result.get("uid").asText())
                    .build();
        }
        return null;
    }

    @SneakyThrows
    private JsonNode createGetStationList() {
        Map<String, Object> queries = new HashMap<>();
        queries.put("language", "en");
        String path = creatHttpPathWithQueries(GET_SOLARMAN_OBTAIN_STATION_LIST_PATH, queries);
        ObjectNode data = JacksonUtil.newObjectNode();
        data.set("page", objectToJsonNode(1));
        data.set("size", objectToJsonNode(10));
        RequestEntity<Object> requestEntity = createRequestWithBody(path, data, HttpMethod.POST, true);
        ResponseEntity<ObjectNode> responseEntity = sendRequest(requestEntity);
        if (responseEntity != null && responseEntity.getBody().get("success").asBoolean()) {
            return responseEntity.getBody();
        }
        return null;
    }


    private SolarmanToken refreshGetSolarmanToken() throws Exception {
        String path = String.format(GET_TUYA_REFRESH_TOKEN_URL_PATH, accessSolarmanToken.getRefreshToken());
//        RequestEntity<Object> requestEntity = createGetTuyaRequest(path, true);
        RequestEntity<Object> requestEntity = null;
        ResponseEntity<ObjectNode> responseEntity = sendRequest(requestEntity);
        if (responseEntity != null) {
            JsonNode result = responseEntity.getBody();
            return SolarmanToken.builder()
                    .accessToken(result.get("access_token").asText())
                    .refreshToken(result.get("refresh_token").asText())
                    .expiresIn(result.get("expires_in").asText())
                    .uid(result.get("uid").asText())
                    .build();
        }
        return null;
    }

    private RequestEntity<Object> createRequestWithBody(String path, ObjectNode body, HttpMethod httpMethod,  boolean withToken) throws Exception {
        String ts = String.valueOf(System.currentTimeMillis());
        MultiValueMap<String, String> httpHeaders = createSolarmanHeaders(ts, withToken);
        URI uri = URI.create(this.solarmanMqttDataConnection.getRegion().getApiUrl() + path);
        return new RequestEntity<>(body.toString(), httpHeaders, httpMethod, uri);
    }

    private HttpHeaders createSolarmanHeaders(String ts, boolean withToken) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("t", ts);
        httpHeaders.add("Content-Type", "application/json");
        if (withToken) {
            httpHeaders.set("Authorization", "bearer " + accessSolarmanToken.getAccessToken());
        }
        return httpHeaders;
    }
}

