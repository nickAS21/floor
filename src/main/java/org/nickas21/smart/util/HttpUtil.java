package org.nickas21.smart.util;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.nickas21.smart.tuya.constant.TuyaApi.EMPTY_HASH;

@Slf4j
public class HttpUtil {
    private static final RestTemplate httpClient = new RestTemplate();

    public static String creatHttpPathWithQueries(String path, Map<String, Object> queries) {
        String pathWithQueries = path;
        pathWithQueries += "?" + queries.entrySet().stream().map(it -> it.getKey() + "=" + it.getValue())
                .collect(Collectors.joining("&"));
        return pathWithQueries;
    }

    //    https://openapi.tuyaeu.com/v1.0/iot-03/devices/bfa715581477683002qb4l/freeze-state
    public static ResponseEntity<ObjectNode> sendRequest(RequestEntity<Object> requestEntity) {

        ResponseEntity<ObjectNode> responseEntity = httpClient.exchange(requestEntity.getUrl(), requestEntity.getMethod(), requestEntity, ObjectNode.class);
        if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
            throw new RuntimeException(String.format("No response for device command request! Reason code from Tuya Cloud: %s", responseEntity.getStatusCode().toString()));
        } else {
            if (Objects.requireNonNull(responseEntity.getBody()).get("success").asBoolean()) {
                return responseEntity;
            } else {
                log.error("cod: [{}], msg: [{}]", responseEntity.getBody().get("code").asInt(), responseEntity.getBody().get("msg").asText());
                return null;
            }
        }
    }

    public static String getBodyHash(String body) throws Exception {
        if (StringUtils.isBlank(body)) {
            return EMPTY_HASH;
        } else {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(body.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(messageDigest.digest());
        }
    }

    public static String get_SHA_256_SecurePassword(String passwordToHash) {
        String generatedPassword = null;
        try {
            String salt = getSalt();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] bytes = md.digest(passwordToHash.getBytes());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16)
                        .substring(1));
            }
            generatedPassword = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return generatedPassword;
    }

    private static String getSalt() throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        return salt.toString();
    }

}
