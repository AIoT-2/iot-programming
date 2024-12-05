package com.nhnacademy.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttCallbackImpl implements MqttCallback{

    private static Logger logger = LoggerFactory.getLogger(MqttCallbackImpl.class);
    private final MessageHandler messageHandler;
    private final MqttClient client;

    public MqttCallbackImpl(MqttClient client, MessageHandler messageHandler){
        this.client = client;
        this.messageHandler = messageHandler;   
    }

    @Override
    public void connectionLost(Throwable cause) {
        logger.warn("Connection lost: {}", cause.getMessage());
        try {
            client.reconnect();
            logger.info("Reconnected to broker");
        } catch (MqttException e) {
            logger.error("Error during connection", e);
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String messagePayload = new String(message.getPayload());

        if(messageHandler != null){
            messageHandler.processMessage(topic, messagePayload);
        } else {
            logger.info("Received message from topic '{}': {}", topic, messagePayload);
        }
    }
    
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        logger.info("Message deliver complete: {}", token.getMessageId());
    }
}
