package com.sensor_data_parsing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;

public class MqttPublisher {
    // 메시지를 받을 브로커
    private static final String MQTT_HOST = "192.168.70.203"; // MQTT 브로커 주소
    private static final String MQTT_USER_NAME = ""; // MQTT 사용자 이름
    private static final String MQTT_PASSWORD = ""; // MQTT 비밀번호

    // 메시지를 보낼 브로커
    private static final String NEW_MQTT_HOST = "localhost"; // MQTT 브로커 주소
    private static final String NEW_MQTT_USER_NAME = ""; // MQTT 사용자 이름
    private static final String NEW_MQTT_PASSWORD = ""; // MQTT 비밀번호

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {

        // MQTT 클라이언트 설정
        final Mqtt5Client mqttClient = Mqtt5Client.builder()
                .identifier("controlcenter-1234") // 클라이언트 식별자
                .serverHost(MQTT_HOST)
                .automaticReconnectWithDefaultConfig() // 자동 재연결
                .serverPort(1883)
                .build();

        // 메시지를 보낼 새로운 MQTT 클라이언트 설정 (메시지를 보낼 브로커)
        final Mqtt5Client newMqttClient = Mqtt5Client.builder()
                .identifier("controlcenter-5678") // 클라이언트 식별자
                .serverHost(NEW_MQTT_HOST)
                .automaticReconnectWithDefaultConfig() // 자동 재연결
                .serverPort(8888)
                .build();

        // MQTT 클라이언트 연결
        mqttClient.toBlocking().connectWith()
                .simpleAuth()
                .username(MQTT_USER_NAME)
                .password(MQTT_PASSWORD.getBytes(StandardCharsets.UTF_8))
                .applySimpleAuth()
                .cleanStart(false)
                .sessionExpiryInterval(TimeUnit.HOURS.toSeconds(1))
                .send();

        // 메시지를 보낼 새로운 MQTT 클라이언트 연결
        newMqttClient.toBlocking().connectWith()
                .simpleAuth()
                .username(NEW_MQTT_USER_NAME)
                .password(NEW_MQTT_PASSWORD.getBytes(StandardCharsets.UTF_8))
                .applySimpleAuth()
                .cleanStart(false)
                .sessionExpiryInterval(TimeUnit.HOURS.toSeconds(1))
                .send();

        // MQTT 토픽 구독 및 메시지 수신
        mqttClient.toAsync().subscribeWith()
                .topicFilter("application/#") // 'application/'로 시작하는 모든 토픽 구독
                .callback(publish -> {
                    String message = new String(publish.getPayloadAsBytes(),
                            StandardCharsets.UTF_8);

                    try {
                        JsonNode rootNode = objectMapper.readTree(message);
                        JsonNode objectNode = rootNode.path("object");

                        if (!objectNode.isMissingNode()) {
                            // deviceName과 spotName 추출
                            String deviceName = rootNode.path("deviceInfo")
                                    .path("deviceName").asText();
                            String spotName = rootNode.path("deviceInfo").path("tags")
                                    .path("name").asText();

                            System.out.println("deviceName: " + deviceName);
                            System.out.println("spotName: " + spotName);

                            // 데이터를 Map으로 변환
                            Map<String, Object> dataMap = objectMapper
                                    .readValue(objectNode.toString(), Map.class);

                            // 메시지를 보낼 새로운 JSON 구조
                            Map<String, Object> finalMessage = Map.of(
                                    "deviceName", deviceName,
                                    "spotName", spotName,
                                    "data", dataMap // 나머지 데이터를 "data" 안에 넣음
                            );

                            // JSON 문자열로 변환
                            String newMessage = objectMapper
                                    .writeValueAsString(finalMessage);

                            // 메시지를 보낼 새 MQTT 브로커로 메시지 발행
                            String newTopic = "application/" + deviceName + "/" + spotName;
                            newMqttClient.toAsync().publishWith()
                                    .topic(newTopic)
                                    .payload(newMessage.getBytes(
                                            StandardCharsets.UTF_8))
                                    .qos(MqttQos.AT_LEAST_ONCE) // QoS 1 (최소 한 번 전송)
                                    .send();
                        }

                    } catch (IOException e) {
                        System.err.println("JSON 파싱 중 오류가 발생: " + e.getMessage());
                    }

                })
                .send();

        // 클라이언트가 계속 실행되도록 대기
        System.out.println("MQTT 메시지를 기다리는 중...");
        while (true) {
            // MQTT 클라이언트가 수신한 메시지 처리 대기
        }
    }
}
