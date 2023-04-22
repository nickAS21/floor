package org.nickas21.smart.tuya.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

@Slf4j
public abstract class BaseController {

//    private final RestTemplate httpClient = new RestTemplate();
//
//    private ResponseEntity<ObjectNode> sendRequest(RequestEntity<Object> requestEntity, HttpMethod httpMethod) {
//        ResponseEntity<ObjectNode> responseEntity = httpClient.exchange(requestEntity.getUrl(), httpMethod, requestEntity, ObjectNode.class);
//        if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
//            throw new RuntimeException(String.format("No response for device command request! Reason code from Tuya Cloud: %s", responseEntity.getStatusCode().toString()));
//        } else {
//            if (Objects.requireNonNull(responseEntity.getBody()).get("success").asBoolean()) {
//                JsonNode result = responseEntity.getBody().get("result");
//                log.info("result: [{}]", result);
//                return responseEntity;
//            } else {
//                log.error("cod: [{}], msg: [{}]", responseEntity.getBody().get("code").asInt(), responseEntity.getBody().get("msg").asText());
//                return null;
//            }
//        }
//    }
}
