package com.example;

import java.util.logging.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttPublisher {
    private static final Logger logger = Logger.getLogger(MqttPublisher.class.getName());
    private final MqttClient mqttClient;
    private final String topicPrefix;

    public MqttPublisher(String brokerUrl, String clientId, String topicPrefix) throws MqttException {
        this.mqttClient = new MqttClient(brokerUrl, clientId);
        this.topicPrefix = topicPrefix;
        
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setConnectionTimeout(60);
        options.setKeepAliveInterval(60);
        options.setAutomaticReconnect(true);
        
        logger.info("Connecting to MQTT broker: " + brokerUrl);
        mqttClient.connect(options);
        logger.info("Connected to MQTT broker");
    }

    // 메시지 발행 메소드
    public void publish(String topic, String payload) throws MqttException {
        String fullTopic = topicPrefix + "/" + topic;
        MqttMessage message = new MqttMessage(payload.getBytes());
        message.setQos(1);
        message.setRetained(true);
        
        logger.info("Publishing to topic: " + fullTopic);
        mqttClient.publish(fullTopic, message);
        logger.info("Published message to topic: " + fullTopic);
    }

    public void close() throws MqttException {
        if (mqttClient.isConnected()) {
            mqttClient.disconnect();
        }
        mqttClient.close();
    }
}