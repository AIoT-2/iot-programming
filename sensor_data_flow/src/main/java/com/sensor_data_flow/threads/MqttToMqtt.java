package com.sensor_data_flow.threads;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sensor_data_flow.client.MqttClient;
import com.sensor_data_flow.interfaces.ProtocolToMqtt;

/**
 * MqttToMqtt 클래스는 MQTT 메시지를 구독하고, 해당 메시지를 처리한 후 새로운 형식으로 발행하는 작업을 수행합니다.
 * Runnable 인터페이스를 구현하여 별도의 스레드에서 실행됩니다.
 */
public class MqttToMqtt implements ProtocolToMqtt {
    private static final ObjectMapper objectMapper = new ObjectMapper(); // JSON 처리에 사용할 ObjectMapper 객체

    // 구독자와 발행자 MqttClient 객체
    private final MqttClient subscriber;
    private final MqttClient publisher;

    private final Object lock = new Object(); // 동기화를 위한 lock 객체
    // 구독이 이미 되어있으면 스레드 생성을 막기 위한 플래그.
    private boolean isSubscribed = false;
    // 메시지를 수신하는데 사용되는 변수. 수신된 메시지를 저장합니다.
    private String receivedMessage;

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

    @Override
    public String fetchDataFromProtocol() {
        receivedMessage = null;
        // 구독을 한 번만 설정하도록 함
        if (!isSubscribed) {
            subscriber.subscribeMessage("application/#", (String message) -> {
                synchronized (lock) {
                    receivedMessage = message;
                    lock.notify(); // 메시지가 수신되면 대기 중인 스레드를 깨웁니다.
                }
            });
            isSubscribed = true; // 한 번만 구독하도록 플래그 설정
        }

        synchronized (lock) {
            // 수신된 메시지가 있을 때까지 대기
            while (receivedMessage == null) {
                try {
                    lock.wait(); // 메시지가 수신될 때까지 대기
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        return receivedMessage; // 수신된 메시지 반환
    }

    @Override
    public String[] convertToMqttFormat(String message) {
        String[] messageArray = new String[2];
        // 수신한 메시지를 처리하는 부분
        try {
            JsonNode rootNode = objectMapper.readTree(message);
            JsonNode objectNode = rootNode.path("object");

            if (!objectNode.isMissingNode()) {
                String deviceName = rootNode.path("deviceInfo")
                        .path("deviceName").asText();
                String spotName = rootNode.path("deviceInfo").path("tags")
                        .path("name").asText();

                logger.info("deviceName: {}", deviceName);
                logger.info("spotName: {}", spotName);

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
            logger.error("JSON 파싱 중 오류가 발생: {}", e.getMessage());
        }

        return messageArray;
    }

    @Override
    public void sendMessageToMqtt(String[] message) {
        // 발행자에게 새로운 메시지 전송
        publisher.sendMessage(message[0], message[1]);
    }
}
