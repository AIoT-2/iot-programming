package com.sensor_data_parsing.threads;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sensor_data_parsing.MqttClient;
import com.sensor_data_parsing.interfaces.ProtocolToMqtt;

/**
 * MqttToMqtt 클래스는 MQTT 메시지를 구독하고, 해당 메시지를 처리한 후 새로운 형식으로 발행하는 작업을 수행합니다.
 * Runnable 인터페이스를 구현하여 별도의 스레드에서 실행됩니다.
 */
public class MqttToMqtt implements ProtocolToMqtt {
    private static final ObjectMapper objectMapper = new ObjectMapper(); // JSON 처리에 사용할 ObjectMapper 객체

    // 구독자와 발행자 MqttClient 객체
    private final MqttClient subscriber;
    private final MqttClient publisher;

    /**
     * MqttToMqtt 생성자는 구독자와 발행자 MqttClient를 초기화합니다.
     * 
     * @param subscriber 메시지를 구독할 MqttClient 객체
     * @param publisher  메시지를 발행할 MqttClient 객체
     */
    public MqttToMqtt(MqttClient subscriber, MqttClient publisher) {
        this.subscriber = subscriber;
        this.publisher = publisher;
    }

    // 구독한 MQTT 토픽에서 메시지를 수신하고, 처리한 후 새로운 형식으로 메시지를 발행합니다.
    @Override
    public String fetchDataFromProtocol() {
        final String[] receivedMessage = new String[1];
        final CountDownLatch latch = new CountDownLatch(1); // 동기화를 위한 카운트다운 래치

        // 메시지를 비동기적으로 수신
        subscriber.subscribeMessage("application/#", (String message) -> {
            receivedMessage[0] = message;
            latch.countDown(); // 메시지가 수신되면 카운트다운
        });

        // 메시지가 수신될 때까지 기다림
        try {
            latch.await(); // 메시지가 수신될 때까지 대기
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // InterruptedException 처리
            System.err.println("메시지 수신 대기 중 인터럽트 발생");
        }

        return receivedMessage[0]; // 수신된 메시지 반환
    }

    @Override
    public String[] convertToMqttFormat(String message) {
        String[] messageArray = new String[2];
        // 수신한 메시지를 처리하는 부분
        try {
            System.out.println(message);
            JsonNode rootNode = objectMapper.readTree(message);
            JsonNode objectNode = rootNode.path("object");

            if (!objectNode.isMissingNode()) {
                String deviceName = rootNode.path("deviceInfo")
                        .path("deviceName").asText();
                String spotName = rootNode.path("deviceInfo").path("tags")
                        .path("name").asText();

                System.out.println("deviceName: " + deviceName);
                System.out.println("spotName: " + spotName);

                // "object" 필드를 Map으로 변환
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = objectMapper
                        .readValue(objectNode.toString(), Map.class);

                Map<String, Object> finalMessage = Map.of(
                        "deviceName", deviceName,
                        "spotName", spotName,
                        "data", dataMap // 나머지 데이터를 "data" 안에 넣음
                );

                messageArray[0] = "application/" + deviceName + "/" + spotName;
                // 새로운 메시지를 JSON 문자열로 변환
                messageArray[1] = objectMapper
                        .writeValueAsString(finalMessage);
            }

        } catch (IOException e) {
            System.err.println("JSON 파싱 중 오류가 발생: " + e.getMessage());
        }

        return messageArray;
    }

    @Override
    public void sendMessageToMqtt(String[] message) {
        // 발행자에게 새로운 메시지 전송
        publisher.sendMessage(message[0], message[1]);
    }
}
