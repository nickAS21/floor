package org.nickas21.smart.tuya.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.nickas21.smart.tuya.TuyaMessageDataSource;
import org.nickas21.smart.tuya.mq.TuyaConnectionMsg;
import org.nickas21.smart.tuya.mq.TuyaToken;
import org.nickas21.smart.tuya.response.Device;
import org.nickas21.smart.tuya.response.Devices;
import org.nickas21.smart.tuya.sourece.ApiDataSource;
import org.nickas21.smart.util.ConnectThreadFactory;
import org.nickas21.smart.util.HmacSHA256Util;
import org.nickas21.smart.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.nickas21.smart.tuya.constant.TuyaApi.EMPTY_HASH;
import static org.nickas21.smart.tuya.constant.TuyaApi.GET_DEVICES_ID_URL_PATH;
import static org.nickas21.smart.tuya.constant.TuyaApi.GET_REFRESH_TOKEN_URL_PATH;
import static org.nickas21.smart.tuya.constant.TuyaApi.GET_STATUS_URL_PATH;
import static org.nickas21.smart.tuya.constant.TuyaApi.GET_TOKEN_URL_PATH;
import static org.nickas21.smart.tuya.constant.TuyaApi.TOKEN_GRANT_TYPE;
import static org.nickas21.smart.util.JacksonUtil.treeToValue;

@Service
@Slf4j
public class DefaultTuDeviceService implements TuDeviceService {

    private TuyaToken accessToken;
    private Devices devices;

    private final RestTemplate httpClient = new RestTemplate();
    private ExecutorService executor;
    private TuyaMessageDataSource connectionConfiguration;

    @Autowired
    private ApiDataSource dataSource;


    public DefaultTuDeviceService(ApiDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void init() {
        executor = Executors.newSingleThreadExecutor(ConnectThreadFactory.forName(getClass().getSimpleName() + "-service"));
        accessToken = getToken();
        devices = new Devices();
        if (accessToken != null) {
            sendInitRequest();
        }
        log.info("init successful: [{}] devices", devices.getDevIds().size());
    }

    public void setConnectionConfiguration(TuyaMessageDataSource connectionConfiguration) {
        this.connectionConfiguration = connectionConfiguration;
    }

    @SneakyThrows
    public void devicesUpDateStatusValue (TuyaConnectionMsg msg) {
        String deviceId = msg.getJson().get("devId").asText();
        JsonNode deviceStatus = msg.getJson().get("status");
        Device device = this.devices.getDevIds().get(deviceId);
        device.setStatus(deviceStatus);
    }

    @SneakyThrows
    private TuyaToken createToken() {
        Future<TuyaToken> future = executor.submit(() -> {
            try {
                return createGetToken();
            } catch (Exception e) {
                log.error("create token error", e);
                return null;
            } finally {
            }
        });
        TuyaToken createToken = future.get();
        if (Objects.isNull(createToken)) {
            log.error("Create token required, not null.");
        }
        return createToken;
    }

    @SneakyThrows
    private TuyaToken createGetToken() {
        Map<String, Object> queries = new HashMap<>();
        queries.put("grant_type", TOKEN_GRANT_TYPE);
        String path = creatPathWithQueries(GET_TOKEN_URL_PATH, queries);
        RequestEntity<Object> requestEntity = createGetRequest(path, true);
        ResponseEntity<ObjectNode> responseEntity = sendRequest(requestEntity, HttpMethod.GET);
        if (responseEntity != null) {
            JsonNode result = responseEntity.getBody().get("result");
            Long expireAt = responseEntity.getBody().get("t").asLong() + result.get("expire_time").asLong() * 1000;
            return TuyaToken.builder()
                    .accessToken(result.get("access_token").asText())
                    .refreshToken(result.get("refresh_token").asText())
                    .uid(result.get("uid").asText())
                    .expireAt(expireAt)
                    .build();
        }
        return null;
    }

    @SneakyThrows
    private TuyaToken refreshToken() {
        Future<TuyaToken> future = executor.submit(() -> {
            try {
                return refreshGetToken();
            } catch (Exception e) {
                log.error("refresh token error", e);
                return null;
            } finally {
            }
        });
        TuyaToken refreshedToken = future.get();
        if (Objects.isNull(refreshedToken)) {
            log.error("Refreshed token required, not null.");
        }
        return refreshedToken;
    }

    private String creatPathWithQueries(String path, Map<String, Object> queries) {
        String pathWithQueries = path;
        pathWithQueries += "?" + queries.entrySet().stream().map(it -> it.getKey() + "=" + it.getValue())
                .collect(Collectors.joining("&"));
        return pathWithQueries;
    }

    private RequestEntity<Object> createGetRequest(String path, boolean isGetToken) throws Exception {
        HttpMethod httpMethod = HttpMethod.GET;
        String ts = String.valueOf(System.currentTimeMillis());
        MultiValueMap<String, String> httpHeaders = createHeaders(ts);
        if (!isGetToken) httpHeaders.add("access_token", accessToken.getAccessToken());
        String strToSign = isGetToken ? this.connectionConfiguration.getAk() + ts + stringToSign(path, getBodyHash(null), httpMethod) :
                this.connectionConfiguration.getAk() + accessToken.getAccessToken() + ts + stringToSign(path, getBodyHash(null), httpMethod);
        String signedStr = sign(strToSign, this.connectionConfiguration.getSk());
        httpHeaders.add("sign", signedStr);
        URI uri = URI.create(this.connectionConfiguration.getRegion().getApiUrl() + path);
        return new RequestEntity<>(httpHeaders, httpMethod, uri);
    }

    private RequestEntity<Object> createPostRequest(String path, ObjectNode body) throws Exception {
        HttpMethod httpMethod = HttpMethod.POST;
        String ts = String.valueOf(System.currentTimeMillis());
        MultiValueMap<String, String> httpHeaders = createHeaders(ts);
        httpHeaders.add("access_token", accessToken.getAccessToken());
        String strToSign = this.connectionConfiguration.getAk() + accessToken.getAccessToken() +
                ts + stringToSign(path, getBodyHash(body.toString()), httpMethod);
        String signedStr = sign(strToSign, this.connectionConfiguration.getAk());
        httpHeaders.add("sign", signedStr);
        URI uri = URI.create(this.connectionConfiguration.getRegion().getApiUrl() + path);
        return new RequestEntity<>(body.toString(), httpHeaders, httpMethod, uri);
    }

    private HttpHeaders createHeaders(String ts) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("client_id", this.connectionConfiguration.getAk());
        httpHeaders.add("t", ts);
        httpHeaders.add("sign_method", "HMAC-SHA256");
        httpHeaders.add("nonhasValidAccessToken()ce", "");
        httpHeaders.add("Content-Type", "application/json");
        return httpHeaders;
    }

    private TuyaToken getToken() {
        if (accessToken != null) {
            if (!hasValidAccessToken()) {
                accessToken = refreshToken();
            }
        } else {
            accessToken = createToken();
        }
        return accessToken;
    }


    private boolean hasValidAccessToken() {
        return accessToken.getExpireAt() + 20_000 > System.currentTimeMillis();
    }

    private ResponseEntity<ObjectNode> sendRequest(RequestEntity<Object> requestEntity, HttpMethod httpMethod) {
        ResponseEntity<ObjectNode> responseEntity = httpClient.exchange(requestEntity.getUrl(), httpMethod, requestEntity, ObjectNode.class);
        if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
            throw new RuntimeException(String.format("No response for device command request! Reason code from Tuya Cloud: %s", responseEntity.getStatusCode().toString()));
        } else {
            if (Objects.requireNonNull(responseEntity.getBody()).get("success").asBoolean()) {
//                JsonNode result = responseEntity.getBody().get("result");
//                log.info("result: [{}]", result);
                return responseEntity;
            } else {
                log.error("cod: [{}], msg: [{}]", responseEntity.getBody().get("code").asInt(), responseEntity.getBody().get("msg").asText());
                return null;
            }
        }
    }

    private String stringToSign(String path, String bodyHash, HttpMethod httpMethod) throws Exception {
        List<String> lines = new ArrayList<>(16);
        lines.add(httpMethod.name());
        lines.add(bodyHash);
        lines.add("");
        lines.add(path);
        return String.join("\n", lines);
    }

    private String getBodyHash(String body) throws Exception {
        if (StringUtils.isBlank(body)) {
            return EMPTY_HASH;
        } else {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(body.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(messageDigest.digest());
        }
    }

    private String sign(String content, String secret) throws Exception {
        byte[] rawHmac = HmacSHA256Util.sign(content, secret.getBytes(StandardCharsets.UTF_8));
        return Hex.encodeHexString(rawHmac).toUpperCase();
    }

    private TuyaToken refreshGetToken() throws Exception {
        String path = String.format(GET_REFRESH_TOKEN_URL_PATH, accessToken.getRefreshToken());
        RequestEntity<Object> requestEntity = createGetRequest(path, true);
        ResponseEntity<ObjectNode> responseEntity = sendRequest(requestEntity, HttpMethod.GET);
        if (responseEntity != null) {
            JsonNode result = responseEntity.getBody().get("result");
            Long expireAt = responseEntity.getBody().get("t").asLong() + result.get("expire_time").asLong() * 1000;
            return TuyaToken.builder()
                    .accessToken(result.get("access_token").asText())
                    .refreshToken(result.get("refresh_token").asText())
                    .uid(result.get("uid").asText())
                    .expireAt(expireAt)
                    .build();
        }
        return null;
    }


    private void sendPostRequest(String path, ObjectNode commandsNode) throws Exception {
        RequestEntity<Object> requestEntity = createPostRequest(path, commandsNode);
        ResponseEntity<ObjectNode> responseEntity = sendRequest(requestEntity, HttpMethod.POST);
        if (responseEntity != null) {
            JsonNode result = responseEntity.getBody().get("result");
            log.info("result: [{}]", result);
        }
    }

    private void sendInitRequest() {
        for (String deviceId : this.connectionConfiguration.getDeviceIds()) {
            try {
                String path = String.format(GET_DEVICES_ID_URL_PATH, deviceId);
                RequestEntity<Object> requestEntity = createGetRequest(path, false);
                ResponseEntity<ObjectNode> responseEntity = sendRequest(requestEntity, HttpMethod.GET);
                if (responseEntity != null) {
                    JsonNode result = responseEntity.getBody().get("result");
                    Device device = treeToValue(result, Device.class);
                    devices.getDevIds().put(deviceId, device);
                    path = String.format(GET_STATUS_URL_PATH, deviceId);
                    requestEntity = createGetRequest(path, false);
                    responseEntity = sendRequest(requestEntity, HttpMethod.GET);
                    if (responseEntity != null) {
                        result = responseEntity.getBody().get("result");
                        devices.getDevIds().get(deviceId).setStatus(result);
                    }
                }
            } catch (Exception e) {
                log.error("Failed init device with id [{}] [{}]", deviceId, e.getMessage());
            }
        }
    }


    @SneakyThrows
    private void sendGetRequest(String path) {
        RequestEntity<Object> requestEntity = createGetRequest(path, false);
        ResponseEntity<ObjectNode> responseEntity = sendRequest(requestEntity, HttpMethod.GET);
        if (responseEntity != null) {
            JsonNode result = responseEntity.getBody().get("result");
            log.warn("result: [{}]", result);
        }
    }


}
