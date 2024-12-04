package com.nhnacademy.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

@Slf4j
class MqttPropertyTest {

    private final String EXPECT_BROKER = "tcp://192.168.71.219:1883";

    private final String EXPECT_CLIENT_ID = "";

    private final String EXPECT_TOPIC = "application/#";

    @Order(1)
    @Test
    void brokerCheck() {
        // BROKER
        String broker = MqttProperty.getBroker();
        log.debug("Broker: {}", broker);
        Assertions.assertEquals(EXPECT_BROKER, broker);
    }

    @Order(2)
    @Test
    void clientIdCheck() {
        // CLIENT_ID
        String clientId = MqttProperty.getClientId();
        log.debug("ClientId: {}", clientId);
        Assertions.assertEquals(EXPECT_CLIENT_ID, clientId);
    }

    @Order(3)
    @Test
    void topicCheck() {
        // TOPIC
        String topic = MqttProperty.getTopic();
        log.debug("Topic: {}", topic);
        Assertions.assertEquals(EXPECT_TOPIC, topic);
    }
}