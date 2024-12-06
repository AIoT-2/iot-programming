package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.paho.client.mqttv3.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// Mqtt in
public class MqttReceiver {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private String receiverBroker;
    private String clientId;
    private String topic;

    public MqttReceiver() {
        // ConfigReader를 사용하여 프로퍼티 파일에서 설정값을 읽어옵니다.
        ConfigReader configReader = new ConfigReader("application.properties");
        receiverBroker = configReader.getProperty("mqtt.receiver.broker");
        clientId = configReader.getProperty("mqtt.receiver.clientId");
        topic = configReader.getProperty("mqtt.receiver.topic");
    }

    public void startReceiving() {
        try (MqttClient receiverClient = new MqttClient(receiverBroker, clientId)) {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            // 메시지 수신 콜백 설정
            receiverClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.debug("MQTT 연결이 끊어졌습니다. {}", cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // 수신한 메시지 출력
                    String payload = new String(message.getPayload());
                    processMessage(payload);  // 메시지 처리
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // 메시지 전송 완료 이벤트
                    log.debug("메시지 전송이 완료되었습니다.");
                }
            });

            // 수신용 브로커에 연결
            receiverClient.connect(options);

            // 주제 구독 (mqtt in)
            receiverClient.subscribe(topic);
            log.debug("MQTT 수신 브로커에 연결되고 구독을 시작했습니다.");

            // 10초 대기 후 종료
            Thread.sleep(100000);

            // 클라이언트 종료
            if (receiverClient.isConnected()) {
                receiverClient.disconnect();
                log.debug("Disconnected!");
            }

        } catch (Exception e) {
            log.error("메시지 발행 중 오류 발생: {}", e.getMessage());
        }
    }

    // 수신된 메시지를 처리하는 메서드
    private void processMessage(String payload) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(payload);
        if (rootNode.get("object").has("temperature")) {
            // 온도 값 추출
            double temperature = rootNode.get("object").get("temperature").asDouble();
            long timestampMillis = System.currentTimeMillis();
            long timestamp = timestampMillis / 1000;
            String deviceName = rootNode.get("deviceInfo").get("deviceName").asText();

            // 가공된 메시지 생성
            String processedPayload = mapper.writeValueAsString(new ProcessedMessage(
                    temperature,
                    timestamp,
                    deviceName
                ));

            // 발행용 메시지 전송
            MqttPublisher mqttPublisher = new MqttPublisher();
            mqttPublisher.publishMessage("tcp://192.168.71.204:1883", "sensor/temperature", processedPayload);

            log.debug("발행된 메시지: {} : {}", "sensor/temperature", processedPayload);
        } else {
            log.debug("수신된 메시지에 'temperature' 데이터가 없습니다.");
        }
    }
}
