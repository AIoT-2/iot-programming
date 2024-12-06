package com.nhnacademy.mqtt.impl;

import com.nhnacademy.util.Property;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Slf4j
public class MqttClientImpl implements Runnable {

    private static final String SERVICE_NAME = "mqtt";

    private final String broker;

    private final String clientId;

    private final String topic;

    private final String timeFormat;

    public MqttClientImpl() {
        this(Property.getEndpoint(SERVICE_NAME),
                Property.getClientId(),
                Property.getTopic(),
                Property.getTimeFormat());
    }

    public MqttClientImpl(String broker, String clientId, String topic, String timeFormat) {
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
        if (Objects.isNull(timeFormat)) {
            log.error("timeFormat is Null!");
            throw new RuntimeException("timeFormat is Null!");
        }
        this.broker = broker;
        this.clientId = clientId;
        this.topic = topic;
        this.timeFormat = timeFormat;
    }

    @Override
    public void run() {
        String clientIdAndTime = createDynamicId();

        try (MqttClient mqttClient = new MqttClient(broker, clientIdAndTime)) {
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

    private String createDynamicId() {
        StringBuilder sb = new StringBuilder();
        sb.append(clientId);
        sb.append("_");

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeFormat);
        sb.append(now.format(formatter));

        return sb.toString();
    }
}
