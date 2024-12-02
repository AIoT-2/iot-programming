package com.nhnacademy.mqtt;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MqttPropertyTest {

    private final String EXPECT_BROKER = "tcp://192.168.71.219:1883";

    private final String EXPECT_CLIENT_ID = "";

    private final String EXPECT_TOPIC = "application/#";

    @Order(1)
    @Test
    void brokerCheck() {
        // BROKER
        Assertions.assertEquals(EXPECT_BROKER, MqttProperty.getBroker());
    }

    @Order(2)
    @Test
    void clientIdCheck() {
        // CLIENT_ID
        Assertions.assertEquals(EXPECT_CLIENT_ID, MqttProperty.getClientId());
    }

    @Order(3)
    @Test
    void topicCheck() {
        // TOPIC
        Assertions.assertEquals(EXPECT_TOPIC, MqttProperty.getTopic());
    }
}