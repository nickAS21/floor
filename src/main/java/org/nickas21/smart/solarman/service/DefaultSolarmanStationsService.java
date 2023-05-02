package org.nickas21.smart.solarman.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.SmartSolarmanTuyaService;
import org.nickas21.smart.solarman.mq.Communication;
import org.nickas21.smart.solarman.mq.RealTimeData;
import org.nickas21.smart.solarman.mq.RealTimeDataValue;
import org.nickas21.smart.solarman.mq.SolarmanToken;
import org.nickas21.smart.solarman.mq.Station;
import org.nickas21.smart.solarman.source.SolarmanDataSource;
import org.nickas21.smart.util.JacksonUtil;
import org.nickas21.smart.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.nickas21.smart.solarman.constant.SolarmanApi.POST_SOLARMAN_DEVICE_COMMUNICATION_PATH;
import static org.nickas21.smart.solarman.constant.SolarmanApi.POST_SOLARMAN_OBTAIN_PLANT_LIST_PATH;
import static org.nickas21.smart.solarman.constant.SolarmanApi.POST_SOLARMAN_OBTAIN_TOKEN_C_PATH;
import static org.nickas21.smart.solarman.constant.SolarmanApi.POST_SOLARMAN_REALTIME_DATA_PATH;
import static org.nickas21.smart.tuya.constant.TuyaApi.GET_TUYA_REFRESH_TOKEN_URL_PATH;
import static org.nickas21.smart.util.HttpUtil.bmsSoc;
import static org.nickas21.smart.util.HttpUtil.creatHttpPathWithQueries;
import static org.nickas21.smart.util.HttpUtil.formatter;
import static org.nickas21.smart.util.HttpUtil.sendRequest;
import static org.nickas21.smart.util.JacksonUtil.objectToJsonNode;
import static org.nickas21.smart.util.JacksonUtil.treeToValue;

@Slf4j
@Service
public class DefaultSolarmanStationsService implements SolarmanStationsService {
    private ExecutorService executor;
    private SolarmanDataSource solarmanDataSource;
    private SolarmanToken accessSolarmanToken;
    private Map<Long, Station> stations;
    private Map<String, Communication> communications;
//    private Map<Long, RealTimeData> realTimeDatas;
//    private Long bmsSocTimeCurrent;
//    private Long bmsSocTimePrevious;

    @Override
    public void setExecutorService(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public void setSolarmanMqttDataSource(SolarmanDataSource solarmanMqttDataConnection) {
        this.solarmanDataSource = solarmanMqttDataConnection;
    }

    @Override
    public void init() {
        accessSolarmanToken = getSolarmanToken();
        if (accessSolarmanToken != null) {
            getStaionList();
            if (stations.size() > 0) {
                Long stationId = Long.valueOf(stations.keySet().toArray()[0].toString());
                String stationName = stations.get(stationId).getName();
                solarmanDataSource.setStationId(stationId);
                solarmanDataSource.setName(stationName);
                log.info("First station id: [{}], name [{}]", stationId, stationName);
                String loggerSn = this.solarmanDataSource.getLoggerSn();
                getDeviceCommunication(loggerSn);
            } else {
                log.error("Station size is 0");
            }
        }
    }

    @Override
    public SolarmanDataSource getSolarmanDataSource() {
        return this.solarmanDataSource;
    }

    @SneakyThrows
    private SolarmanToken getSolarmanToken() {
        if (accessSolarmanToken != null) {
//            if (!hasValidAccessToken()) {
//                accessSolarmanToken = refreshSolarmanToken();
//            }
        } else {
            String ts = String.valueOf(System.currentTimeMillis());
            MultiValueMap<String, String> httpHeaders = createSolarmanHeaders(ts);
            Map<String, Object> queries = createQueries();
            queries.put("appId", this.solarmanDataSource.getAppId());
            ObjectNode body = JacksonUtil.newObjectNode();
            body.set("appSecret", objectToJsonNode(this.solarmanDataSource.getSecret()));
            body.set("email", objectToJsonNode(this.solarmanDataSource.getUserName()));
            body.set("password", objectToJsonNode(this.solarmanDataSource.getPassHash()));
            RequestEntity<Object> requestEntity = createSolarmanRequest(POST_SOLARMAN_OBTAIN_TOKEN_C_PATH, httpHeaders, HttpMethod.POST, queries, body);
            JsonNode result = requestFutureSend(requestEntity);
            if (Objects.isNull(result)) {
                log.error("Create solarman token required, not null.");
            } else {
                accessSolarmanToken = SolarmanToken.builder()
                        .accessToken(result.get("access_token").asText())
                        .refreshToken(result.get("refresh_token").asText())
                        .expiresIn(result.get("expires_in").asText())
                        .uid(result.get("uid").asText())
                        .build();
            }
        }
        return accessSolarmanToken;
    }

    @SneakyThrows
    private void getStaionList() {
        stations = new ConcurrentHashMap<>();
        String ts = String.valueOf(System.currentTimeMillis());
        MultiValueMap<String, String> httpHeaders = createSolarmanHeadersWithToken(ts);
        Map<String, Object> queries = createQueries();
        ObjectNode body = JacksonUtil.newObjectNode();
        body.set("page", objectToJsonNode(1));
        body.set("size", objectToJsonNode(10));
        RequestEntity<Object> requestEntity = createSolarmanRequest(POST_SOLARMAN_OBTAIN_PLANT_LIST_PATH, httpHeaders, HttpMethod.POST, queries, body);
        JsonNode result = requestFutureSend(requestEntity);
        if (Objects.isNull(result)) {
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
    private void getDeviceCommunication(String loggerSn) {
        communications = new ConcurrentHashMap<>();
        String ts = String.valueOf(System.currentTimeMillis());
        MultiValueMap<String, String> httpHeaders = createSolarmanHeadersWithToken(ts);
        Map<String, Object> queries = createQueries();
        ObjectNode body = JacksonUtil.newObjectNode();
        body.set("deviceSn", objectToJsonNode(loggerSn));
        RequestEntity<Object> requestEntity = createSolarmanRequest(POST_SOLARMAN_DEVICE_COMMUNICATION_PATH, httpHeaders, HttpMethod.POST, queries, body);
        JsonNode result = requestFutureSend(requestEntity);
        if (Objects.isNull(result)) {
            log.error("Create solarman device communication required, not null.");
        } else {
            ObjectNode communicationNode = (ObjectNode) result.get("communication");
            if (communicationNode != null) {
                Communication communication = treeToValue(communicationNode, Communication.class);
                communications.put(this.solarmanDataSource.getLoggerSn(), communication);
                boolean isLoggerInverterError = false;
                if (StringUtils.isNoneBlank(communications.get(loggerSn).getChildList().get(0).getDeviceSn())) {
                    solarmanDataSource.setInverterSn(communications.get(loggerSn).getChildList().get(0).getDeviceSn());
                } else {
                    isLoggerInverterError = true;
                    log.error("Create solarman Inverter Sn required, not null.");
                }
                if (communications.get(loggerSn).getChildList().get(0).getDeviceId() > 0) {
                    solarmanDataSource.setInverterId(communications.get(loggerSn).getChildList().get(0).getDeviceId());
                } else {
                    isLoggerInverterError = true;
                    log.error("Create solarman Inverter Id required, not zero.");
                }
                if (communications.get(loggerSn).getDeviceId() > 0) {
                    solarmanDataSource.setLoggerId(communications.get(loggerSn).getDeviceId());
                } else {
                    isLoggerInverterError = true;
                    log.error("Create solarman Logger Id required, not zero.");
                }
                if (!isLoggerInverterError) {
                    log.info("InverterSn: [{}],  InverterId: [{}],  LoggerId: [{}]",
                            solarmanDataSource.getInverterSn(), solarmanDataSource.getInverterId(),
                            solarmanDataSource.getLoggerId());
                }
            }
        }
    }

    @SneakyThrows
    @Override
    public RealTimeData getRealTimeDataStart() {
        String ts = String.valueOf(System.currentTimeMillis());
        MultiValueMap<String, String> httpHeaders = createSolarmanHeadersWithToken(ts);
        Map<String, Object> queries = createQueries();
        ObjectNode body = JacksonUtil.newObjectNode();
        body.set("deviceSn", objectToJsonNode(this.solarmanDataSource.getInverterSn()));
        body.set("deviceId", objectToJsonNode(this.solarmanDataSource.getInverterId()));
        RequestEntity<Object> requestEntity = createSolarmanRequest(POST_SOLARMAN_REALTIME_DATA_PATH, httpHeaders, HttpMethod.POST, queries, body);
        JsonNode result = requestFutureSend(requestEntity);
        if (Objects.isNull(result)) {
            log.error("Create solarman real time data Sn:[{}] Id: [{}] required, not null.",
                    this.solarmanDataSource.getInverterSn(), this.solarmanDataSource.getInverterId());
            return null;
        } else {
            return treeToValue(result, RealTimeData.class);
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
    private RequestEntity<Object>createSolarmanRequest(String pathRequest, MultiValueMap<String, String> httpHeaders,
                                           HttpMethod httpMethod, Map<String, Object> queries, ObjectNode body) {
        String path = creatHttpPathWithQueries(pathRequest, queries);
        return createRequestWithBody(path, body, httpMethod, httpHeaders);
    }

    private JsonNode requestFutureSend(RequestEntity<Object> requestEntity) {
        Future<ResponseEntity<ObjectNode>> future = executor.submit(() -> {
            try {
                return sendRequest(requestEntity);
            } catch (Exception e) {
                log.error("Create solarman request [{}] error", requestEntity, e);
                return null;
            } finally {
            }
        });
        try {
            ResponseEntity<ObjectNode> responseEntity = future.get();
            if (responseEntity != null && responseEntity.getBody().get("success").asBoolean()) {
                return responseEntity.getBody();
            }
            return null;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

//    @SneakyThrows
//    private JsonNode createGetStationList() {
//        Map<String, Object> queries = new HashMap<>();
//        queries.put("language", "en");
//        String path = creatHttpPathWithQueries(POST_SOLARMAN_OBTAIN_PLANT_LIST_PATH, queries);
//        ObjectNode data = JacksonUtil.newObjectNode();
//        data.set("page", objectToJsonNode(1));
//        data.set("size", objectToJsonNode(10));
//        RequestEntity<Object> requestEntity = createRequestWithBody(path, data, HttpMethod.POST);
//        ResponseEntity<ObjectNode> responseEntity = sendRequest(requestEntity);
//        if (responseEntity != null && responseEntity.getBody().get("success").asBoolean()) {
//            return responseEntity.getBody();
//        }
//        return null;
//    }


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

    private RequestEntity<Object> createRequestWithBody(String path, ObjectNode body, HttpMethod httpMethod, MultiValueMap<String, String> httpHeaders) throws Exception {
        URI uri = URI.create(this.solarmanDataSource.getRegion().getApiUrl() + path);
        return new RequestEntity<>(body.toString(), httpHeaders, httpMethod, uri);
    }

    private HttpHeaders createSolarmanHeaders(String ts) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("t", ts);
        httpHeaders.add("Content-Type", "application/json");
//            httpHeaders.set("Authorization", "bearer " + accessSolarmanToken.getAccessToken());
        return httpHeaders;
    }

    private HttpHeaders createSolarmanHeadersWithToken(String ts) {
        HttpHeaders httpHeaders = createSolarmanHeaders(ts);
        httpHeaders.set("Authorization", "bearer " + accessSolarmanToken.getAccessToken());
        return httpHeaders;
    }

    private Map<String, Object> createQueries() {
        Map<String, Object> queries = new HashMap<>();
        queries.put("language", "en");
        return queries;
    }


}

