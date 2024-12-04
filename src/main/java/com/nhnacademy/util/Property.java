package com.nhnacademy.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * MQTT Client 속성 값 <br>
 * {@code /resources/config.json} 참조
 */
@Slf4j
public final class Property {

    private static final String PATH = "/config.json";

    private static final String BROKER;

    private static final String CLIENT_ID;

    private static final String TOPIC;

    private static final String RETURN = "";

    static {
        try {
            // BROKER = mqttNode.get("broker").asText();
            InputStream inputStream = Property.class.getResourceAsStream(PATH);
            if (Objects.isNull(inputStream)) {
                log.error("'config.json'이 존재하지 않습니다.");
                throw new RuntimeException();
            }
            JsonNode mqttNode = new ObjectMapper()
                                    .readTree(inputStream)
                                    .path("mqtt");
            BROKER = mqttNode.get("ip").asText() + ":" + mqttNode.get("port").asText();
            CLIENT_ID = mqttNode.get("client_id").asText();
            TOPIC = mqttNode.get("topic").asText();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /* static record */

    /**
     * @return MQTT 브로커 주소
     */
    public static String getBroker() {
        return BROKER;
    }

    /**
     * @return 클라이언트 ID
     */
    public static String getClientId() {
        return CLIENT_ID;
    }

    /**
     * @return 구독 및 발행 주제
     */
    public static String getTopic() {
        return TOPIC;
    }
}
