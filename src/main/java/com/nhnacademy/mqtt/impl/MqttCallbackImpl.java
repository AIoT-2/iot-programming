package com.nhnacademy.mqtt.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

@Slf4j
public class MqttCallbackImpl implements MqttCallback {

    @Override
    public void connectionLost(Throwable cause) {
        log.debug("Connection lost: {}",
                cause.getMessage());
    }

    // TODO: jsonNode 데이터를 분석할 멀티스레딩을 구현할 것
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        log.debug("Received message from topic '{}': {}",
                topic, new String(message.getPayload()));
        JsonNode jsonNode = new ObjectMapper()
                                .readTree(message.getPayload());
        log.debug("{}", jsonNode);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        log.debug("Message delivery complete: {}",
                token.getMessageId());
    }
}
