package com.nhnacademy.mqtt.impl;

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

    // MQTT 응답을 받는 곳
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        log.debug("Received message from topic '{}': {}",
                topic, new String(message.getPayload()));
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        log.debug("Message delivery complete: {}",
                token.getMessageId());
    }
}
