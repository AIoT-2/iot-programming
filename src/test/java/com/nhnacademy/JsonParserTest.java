package com.nhnacademy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.util.MqttProperty;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
class JsonParserTest {

    private static JsonNode mqttNode = null;

    private static final String PATH = "/config.json";

    private final String EXPECT_BROKER = "tcp://192.168.71.219:1883";

    private final String EXPECT_CLIENT_ID = "";

    private final String EXPECT_TOPIC = "application/#";

    @BeforeAll
    static void setUp() {
        try (InputStream inputStream = MqttProperty.class.getResourceAsStream(PATH)) {
            mqttNode = new ObjectMapper()
                            .readTree(inputStream)
                            .path("mqtt");
        } catch (IOException e) {
            log.error("{}", e.getMessage(), e);
        }
    }

    @Order(1)
    @Test
    void brokerCheck() {
        // BROKER
        String broker = mqttNode.get("broker").asText();
        log.debug("Broker: {}", broker);
        Assertions.assertEquals(EXPECT_BROKER, broker);
    }

    @Order(2)
    @Test
    void clientIdCheck() {
        // CLIENT_ID
        String clientId = mqttNode.get("client_id").asText();
        log.debug("ClientId: {}", clientId);
        Assertions.assertEquals(EXPECT_CLIENT_ID, clientId);
    }

    @Order(3)
    @Test
    void topicCheck() {
        // TOPIC
        String topic = mqttNode.get("topic").asText();
        log.debug("Topic: {}", topic);
        Assertions.assertEquals(EXPECT_TOPIC, topic);
    }
}
