package com.nhnacademy.nhnacademy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;




public class PracticeControlCenter {
    public static void main(String[] args) {
        final String host = "127.0.0.1";
        final String username = "";
        final String password = "";

        // 1. MQTT 클라이언트 생성
        final Mqtt5Client client = Mqtt5Client.builder()
                .identifier("controlcenter-1234")
                .serverHost(host)
                .automaticReconnectWithDefaultConfig()
                .serverPort(1883)
                .build();

        // 2. MQTT 브로커 연결
        client.toBlocking().connectWith()
                .simpleAuth()
                .username(username)
                .password(password.getBytes(StandardCharsets.UTF_8))
                .applySimpleAuth()
                .cleanStart(false)
                .sessionExpiryInterval(TimeUnit.HOURS.toSeconds(1))
                .send();

        // 3. MQTT 메시지 구독 및 처리
        client.toAsync().subscribeWith()
                .topicFilter("sensor/#")
                .callback(publish -> {
                    try {
                        // 수신된 메시지 출력
                        String topic = publish.getTopic().toString();
                        String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
                        System.out.println("Received message on topic: " + topic);
                        System.out.println("Payload: " + payload);

                        // JSON 데이터 파싱 및 처리
                        processPayload(payload);

                    } catch (Exception e) {
                        System.err.println("Error processing message: " + e.getMessage());
                        e.printStackTrace();
                    }
                })
                .send();
    }

    /**
     * JSON 페이로드를 처리하는 메서드
     *
     * @param payload MQTT 메시지의 JSON 페이로드
     */
    private static void processPayload(String payload) {
        try {
            // Jackson ObjectMapper를 사용해 JSON 파싱
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(payload);

            // 1. 기본 정보 출력
            System.out.println("=== Basic Information ===");
            System.out.println("Deduplication ID: " + rootNode.path("deduplicationId").asText());
            System.out.println("Timestamp: " + rootNode.path("time").asText());

            // 2. 장치 정보 출력
            JsonNode deviceInfo = rootNode.path("deviceInfo");
            if (!deviceInfo.isMissingNode()) {
                System.out.println("\n=== Device Information ===");
                System.out.println("Device Name: " + deviceInfo.path("deviceName").asText());
                JsonNode tags = deviceInfo.path("tags");
                if (!tags.isMissingNode()) {
                    System.out.println("Tags: " + tags.toString());
                }
            }

            // 3. 센서 데이터 출력
            JsonNode objectNode = rootNode.path("object");
            if (!objectNode.isMissingNode()) {
                System.out.println("\n=== Sensor Data ===");
                objectNode.fields().forEachRemaining(field -> {
                    System.out.println(field.getKey() + ": " + field.getValue().asText());
                });
            }

            // 4. 게이트웨이 정보 출력
            JsonNode rxInfo = rootNode.path("rxInfo");
            if (rxInfo.isArray()) {
                System.out.println("\n=== Gateway Information ===");
                for (JsonNode gateway : rxInfo) {
                    System.out.println("Gateway ID: " + gateway.path("gatewayId").asText());
                    System.out.println("Uplink ID: " + gateway.path("uplinkId").asInt());
                }
            }

            // 5. 송신 정보 출력
            JsonNode txInfo = rootNode.path("txInfo");
            if (!txInfo.isMissingNode()) {
                System.out.println("\n=== Transmission Info ===");
                System.out.println("Frequency: " + txInfo.path("frequency").asLong());
            }

            // 6. JSON 파일로 저장 (덧붙이기 모드)
            saveJsonToFile(rootNode);

        } catch (Exception e) {
            System.err.println("Error parsing JSON payload: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * JSON 데이터를 파일에 덧붙여서 저장하는 메서드
     *
     * @param rootNode 처리한 JSON 데이터
     */
    private static void saveJsonToFile(JsonNode rootNode) {
        try {
            // 파일 경로 지정
            File file = new File("output.json");

            // FileWriter를 true 모드로 설정하여 파일 덧붙이기
            FileWriter writer = new FileWriter(file, true);
            writer.write(rootNode.toString() + "\n"); // 데이터를 한 줄로 추가
            writer.close();
            System.out.println("Data saved to output.json");

        } catch (IOException e) {
            System.err.println("Error saving JSON data to file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
