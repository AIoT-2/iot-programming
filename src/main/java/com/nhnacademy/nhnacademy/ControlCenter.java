package com.nhnacademy.nhnacademy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.client.mqtt.datatypes.MqttTopic;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class ControlCenter {

    public static void main(String[] args) {
        final String host = "192.168.70.203"; // MQTT broker IP or hostname
        final String username = ""; // your MQTT username
        final String password = ""; // your MQTT password

        final String outputTopic = "sensor/data"; // Topic for re-publishing data

        // 1. Create the MQTT client
        final Mqtt5Client client = Mqtt5Client.builder()
                .identifier("controlcenter-1234")
                .serverHost(host)
                .automaticReconnectWithDefaultConfig()
                .serverPort(1883)
                .build();

        // 2. Connect the client
        client.toBlocking().connectWith()
                .simpleAuth()
                .username(username)
                .password(password.getBytes(StandardCharsets.UTF_8))
                .applySimpleAuth()
                .cleanStart(false)
                .sessionExpiryInterval(TimeUnit.HOURS.toSeconds(1))
                .send();

        // 3. Subscribe and process messages
        client.toAsync().subscribeWith()
                .topicFilter("application/#")
                .callback(publish -> {
                    String message = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
                    System.out.println("Received message: " + message);

                    // Parse JSON and extract fields
                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        JsonNode rootNode = objectMapper.readTree(message);

                        // deviceName과 spotName 추출
                        String deviceName = rootNode.path("deviceInfo").path("deviceName").asText();
                        String spotName = rootNode.path("tags").path("place").asText();

                        System.out.println("deviceName: " + deviceName);
                        System.out.println("spotName: " + spotName);

                        // Publish the processed message to the output topic
                        Mqtt5Publish publishMessage = Mqtt5Publish.builder()
                                .topic(MqttTopic.of(outputTopic))
                                .payload(message.getBytes(StandardCharsets.UTF_8))
                                .build();

                        client.toAsync().publish(publishMessage).whenComplete((publishResult, throwable) -> {
                            if (throwable != null) {
                                System.err.println("Failed to publish message: " + throwable.getMessage());
                            } else {
                                System.out.println("Published processed message: " + message);
                            }
                        });

                    } catch (Exception e) {
                        System.err.println("Failed to process message: " + e.getMessage());
                    }
                })
                .send();
    }
}
