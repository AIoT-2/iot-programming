package com.nhnacademy.mqtt.impl;

import com.nhnacademy.util.MqttProperty;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import java.util.Objects;

@Slf4j
public class MqttClientImpl implements Runnable {

    private String broker;

    private String clientId;

    private String topic;

    public MqttClientImpl() {
        this(MqttProperty.getBroker(),
                MqttProperty.getClientId(),
                MqttProperty.getTopic());
    }

    public MqttClientImpl(String broker, String clientId, String topic) {
        if (Objects.isNull(broker)) {
            log.error("broker is Null!");
            throw new RuntimeException("broker is Null!");
        }
        if (Objects.isNull(clientId)) {
            log.error("clientId is Null!");
            throw new RuntimeException("clientId is Null!");
        }
        if (Objects.isNull(topic)) {
            log.error("topic is Null!");
            throw new RuntimeException("topic is Null!");
        }
        this.broker = broker;
        this.clientId = clientId;
        this.topic = topic;
    }

    @Override
    public void run() {
        try (MqttClient mqttClient = new MqttClient(broker, clientId)) {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            mqttClient.setCallback(new MqttCallbackImpl());

            log.info("Connecting to broker...");
            mqttClient.connect(options);
            log.info("Connected!");

            log.info("Subscribing to topic: {}", topic);
            mqttClient.subscribe(topic);

            Thread.sleep(100000);

            log.info("Disconnecting...");
            mqttClient.disconnect();
            log.info("Disconnected!");
        } catch (Exception e) {
            log.error("{}", e.getMessage(), e);
        }
    }
}
