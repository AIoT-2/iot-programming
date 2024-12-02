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
    private static final String mqttHost = "192.168.70.203"; // MQTT 브로커 주소
    private static final String mqttUsername = ""; // MQTT 사용자 이름
    private static final String mqttPassword = ""; // MQTT 비밀번호

    // 메시지를 보낼 브로커
    private static final String newMqttHost = "localhost"; // MQTT 브로커 주소
    private static final String newMqttUsername = ""; // MQTT 사용자 이름
    private static final String newMqttPassword = ""; // MQTT 비밀번호

    public static void main(String[] args) {

        // MQTT 클라이언트 설정
        final Mqtt5Client mqttClient = Mqtt5Client.builder()
                .identifier("controlcenter-1234") // 클라이언트 식별자
                .serverHost(mqttHost)
                .automaticReconnectWithDefaultConfig() // 자동 재연결
                .serverPort(1883)
                .build();

        // 메시지를 보낼 새로운 MQTT 클라이언트 설정 (메시지를 보낼 브로커)
        final Mqtt5Client newMqttClient = Mqtt5Client.builder()
                .identifier("controlcenter-5678") // 클라이언트 식별자
                .serverHost(newMqttHost)
                .automaticReconnectWithDefaultConfig() // 자동 재연결
                .serverPort(8888)
                .build();

        // MQTT 클라이언트 연결
        mqttClient.toBlocking().connectWith()
                .simpleAuth()
                .username(mqttUsername)
                .password(mqttPassword.getBytes(StandardCharsets.UTF_8))
                .applySimpleAuth()
                .cleanStart(false)
                .sessionExpiryInterval(TimeUnit.HOURS.toSeconds(1))
                .send();

        // 메시지를 보낼 새로운 MQTT 클라이언트 연결
        newMqttClient.toBlocking().connectWith()
                .simpleAuth()
                .username(newMqttUsername)
                .password(newMqttPassword.getBytes(StandardCharsets.UTF_8))
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

                    // JSON 파싱
                    ObjectMapper objectMapper = new ObjectMapper();
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

                            // 데이터 출력
                            System.out.println("----------data----------");
                            for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
                                System.out.println(entry.getKey() + ": "
                                        + entry.getValue());
                            }

                            // 메시지를 보낼 새 MQTT 브로커로 메시지 전송
                            String newTopic = "application/" + deviceName + "/" + spotName;
                            String newMessage = objectMapper.writeValueAsString(dataMap);

                            // 메시지를 보낼 새 MQTT 브로커로 메시지 발행
                            newMqttClient.toAsync().publishWith()
                                    .topic(newTopic)
                                    .payload(newMessage.getBytes(StandardCharsets.UTF_8))
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
