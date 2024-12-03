package com.example.mqtt_mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.*;

public class Main {
    // MQTT 브로커 주소
    private static final String BROKER = "tcp://192.168.70.203:1883";
    private static final String CLIENT_ID = "JavaClientExample";
    private static final String SUBSCRIBE_TOPIC = "data/#"; // 구독 주제
    private static final String PUBLISH_TOPIC = "processed/data"; // 발행 주제

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        try (MqttClient client = new MqttClient(BROKER, CLIENT_ID)) {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Connection lost: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    try {
                        // 수신 메시지 출력
                        String payload = new String(message.getPayload());
                        System.out.println("Received message: " + payload);

                        // JSON 파싱
                        JsonNode rootNode = objectMapper.readTree(payload);

                        // 필요한 데이터 추출
                        String deduplicationId = rootNode.path("deduplicationId").asText();
                        String time = rootNode.path("time").asText();
                        String devicename = rootNode.path("deviceInfo").path("deviceName").asText();
                        double temperature = rootNode.path("object").path("temperature").asInt();
                        double humidity = rootNode.path("object").path("humidity").asInt();

                        System.out.println(topic);
                        System.out.println(time);
                        System.out.println(temperature);
                        System.out.println(humidity);
                        System.out.println(devicename);
                        System.out.println(deduplicationId);

                        // 가공된 메시지 생성
                        TransformedData transformedData = new TransformedData(
                                topic,
                                time,
                                temperature,
                                humidity,
                                devicename,
                                deduplicationId
                        );

                        // JSON으로 변환
                        String transformedMessage = objectMapper.writeValueAsString(transformedData);
                        System.out.println("Transformed message: " + transformedMessage);

                        // 변환된 메시지 발행
                        client.publish(PUBLISH_TOPIC, new MqttMessage(transformedMessage.getBytes()));
                        System.out.println("Published to topic: " + PUBLISH_TOPIC);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("Message delivery complete: " + token.getMessageId());
                }
            });

            // MQTT 브로커 연결
            System.out.println("Connecting to broker...");
            client.connect(options);
            System.out.println("Connected!");

            // 주제 구독
            System.out.println("Subscribing to topic: " + SUBSCRIBE_TOPIC);
            client.subscribe(SUBSCRIBE_TOPIC);

            // 대기 (10분간 실행 유지)
            Thread.sleep(600_000);

            // 클라이언트 종료
            System.out.println("Disconnecting...");
            client.disconnect();
            System.out.println("Disconnected!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 가공된 메시지의 구조
    static class TransformedData {
        private String topic;
        private String time;
        private double temperature;
        private double humidity;
        private String devicename;
        private String deduplicationId;

        public TransformedData(String topic, String time, double temperature, double humidity, String devicename, String deduplicationId) {
            this.topic = topic;
            this.time = time;
            this.temperature = temperature;
            this.humidity = humidity;
            this.devicename = devicename;
            this.deduplicationId = deduplicationId;
        }

        public String getTopic() {
            return topic;
        }

        public String getTime() {
            return time;
        }

        public double getTemperature() {
            return temperature;
        }

        public double getHumidity() {
            return humidity;
        }

        public String getDevicename() {
            return devicename;
        }

        public String getDeduplicationId() {
            return deduplicationId;
        }
    }
}
