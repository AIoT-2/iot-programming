package com.example.mqtt_mqtt;

import org.eclipse.paho.client.mqttv3.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MqttPublisher {
    private static final String BROKER = "tcp://192.168.70.203:1883";
    private static final String CLIENT_ID = "JavaPublisher";
    private static final String ERRMESSAGE = "ErrorMessage: {}";
    private MqttClient client;

    public MqttPublisher() {
        try {
            client = new MqttClient(BROKER, CLIENT_ID);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            log.info("Connecting to broker for publishing...");
            client.connect(options);
            log.info("Connected to broker for publishing!");
        } catch (MqttException e) {
            log.debug(ERRMESSAGE, e.getMessage());
        }
    }

    public void publish(String topic, String message) {
        try {
            String publishTopic = "sensor/" + topic;
            log.debug("Publishing message to topic: {}", publishTopic);
            log.debug("Publishing msg message: {}", message);
            client.publish(publishTopic, new MqttMessage(message.getBytes()));
        } catch (MqttException e) {
            log.debug(ERRMESSAGE, e.getMessage());
        }
    }

    public void disconnect() {
        try {
            if (client.isConnected()) {
                client.disconnect();
                log.info("Publisher disconnected!");
            }
        } catch (MqttException e) {
            log.debug(ERRMESSAGE, e.getMessage());
        }
    }
}