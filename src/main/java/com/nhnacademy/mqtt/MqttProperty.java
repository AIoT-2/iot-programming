package com.nhnacademy.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

/**
 * MQTT Client 속성 값 <br>
 * {@code /resources/config.json} 참조
 */
public final class MqttProperty {

    private static final String PATH = "src/main/resources/config.json";

    private static final String BROKER;

    private static final String CLIENT_ID;

    private static final String TOPIC;

    private static final String RETURN = "";

    static {
        try {
            JsonNode mqttNode = new ObjectMapper()
                                    .readTree(new File(PATH))
                                    .path("mqtt");
            // BROKER = mqttNode.get("broker").asText();
            BROKER = mqttNode.get("ip").asText() + ":" + mqttNode.get("port").asText();
            CLIENT_ID = mqttNode.get("client_id").asText();
            TOPIC = mqttNode.get("topic").asText();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

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
