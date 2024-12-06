package com.nhnacademy.mqtt;

import java.util.Objects;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MqttCallbackImpl implements MqttCallback{
    private final MessageHandler messageHandler;
    private final MqttClient client;

    public MqttCallbackImpl(MqttClient client, MessageHandler messageHandler){
        if(Objects.isNull(client)){
            throw new IllegalArgumentException("client is null");
        }
        this.client = client;
        this.messageHandler = messageHandler;   
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("Connection lost: {}", cause.getMessage());
        try {
            client.reconnect();
            log.debug("Reconnected to broker");
        } catch (MqttException e) {
            log.error("Error during connection", e);
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String messagePayload = new String(message.getPayload());

        if(messageHandler != null){
            messageHandler.processMessage(topic, messagePayload);
        } else {
            log.debug("Received message from topic '{}': {}", topic, messagePayload);
        }
    }
    
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        log.debug("Message deliver complete: {}", token.getMessageId());
    }
}
