package com.nhnacademy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

@Slf4j
class JsonParserTest {

    private static JsonNode mqttNode = null;

    private static final String PATH = "src/main/resources/config.json";

    private final String EXPECT_BROKER = "tcp://192.168.71.219:1883";

    private final String EXPECT_CLIENT_ID = "";

    private final String EXPECT_TOPIC = "application/#";

    @BeforeAll
    static void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            mqttNode = objectMapper.readTree(new File(PATH)).path("mqtt");
        } catch (IOException e) {
            // log 로그 만들기 실패!
            e.printStackTrace();
        }
    }

    @Order(1)
    @Test
    void brokerCheck() {
        // BROKER
        String broker = mqttNode.get("broker").asText();
        Assertions.assertEquals(EXPECT_BROKER, broker);
    }

    @Order(2)
    @Test
    void clientIdCheck() {
        // CLIENT_ID
        String clientId = mqttNode.get("client_id").asText();
        Assertions.assertEquals(EXPECT_CLIENT_ID, clientId);
    }

    @Order(3)
    @Test
    void topicCheck() {
        // TOPIC
        String topic = mqttNode.get("topic").asText();
        Assertions.assertEquals(EXPECT_TOPIC, topic);
    }
}
