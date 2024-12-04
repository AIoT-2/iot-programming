package com.nhnacademy;

import org.eclipse.paho.client.mqttv3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TransferMqtt {
    // MQTT 브로커 주소 (수신용)
    private static final String RECEIVER_BROKER = "tcp://192.168.70.203:1883";  // mqtt in (수신)
    // MQTT 브로커 주소 (발신용)
    private static final String MOSQUITTO_BROKER = "tcp://192.168.71.204:1883"; // mqtt out (발신)
    private static final String CLIENT_ID = "MyJavaClientExample";
    private static final String TOPIC = "application/#";  // 구독할 주제
    private static final String PUBLISH_TOPIC = "sensor/temperature";  // 발행할 주제
    
    public static void main(String[] args) {
        try (MqttClient receiverClient = new MqttClient(RECEIVER_BROKER, CLIENT_ID)) {  // 수신용 브로커에 연결
            // 연결 설정
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true); // 클린 세션 사용

            // 메시지 수신 콜백 설정
            receiverClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.err.println("MQTT 연결이 끊어졌습니다." + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // 수신한 메시지 출력
                    String payload = new String(message.getPayload());

                    // JSON 파싱
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode rootNode = mapper.readTree(payload);
                    if (rootNode.has("object") && rootNode.get("object").has("temperature")) {
                        // JSON 데이터에서 필요한 값 추출
                        double temperature = rootNode.get("object").get("temperature").asDouble();
                        long timestampMillis = System.currentTimeMillis();  // 밀리초
                        long timestamp = timestampMillis / 1000;    // 초 단위로 변환
                        String deviceName = rootNode.get("deviceInfo").get("deviceName").asText();

                        System.out.println("asdf"+deviceName);

                        // 가공된 메시지 생성
                        String processedPayload = mapper.writeValueAsString(new ProcessedMessage(
                            temperature,
                            timestamp,
                            deviceName
                        ));

                        // Mosquitto 브로커로 메시지 발행 (mqtt out)
                        publishMessage(MOSQUITTO_BROKER, PUBLISH_TOPIC, processedPayload);  // 발신용 브로커에 발행

                        System.out.printf("발행된 메시지: %s : %s%n", PUBLISH_TOPIC, processedPayload);
                    } else {
                        System.out.println("수신된 메시지에 'temperature' 데이터가 없습니다.");
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // 메시지 전송 완료 이벤트
                    System.out.println("메시지 전송이 완료되었습니다.");
                }
            });

            // 수신용 브로커에 연결
            receiverClient.connect(options);

            // 주제 구독 (mqtt in)
            receiverClient.subscribe(TOPIC);
            System.out.println("MQTT 수신 브로커에 연결되고 구독을 시작했습니다.");

            // 10초 대기 후 종료
            Thread.sleep(100000);

            // 클라이언트 종료
            if (receiverClient.isConnected()) {
                try {
                    System.out.println("Disconnecting...");
                    receiverClient.disconnect();
                    System.out.println("Disconnected!");
                } catch (MqttException e) {
                    System.err.println("Error during disconnect: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("Client is already disconnected.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // MQTT 메시지 발행 메서드 (Mosquitto 브로커로 발행)
    private static void publishMessage(String broker, String topic, String message) {
        try (MqttClient mqttClient = new MqttClient(broker, CLIENT_ID)) { // Mosquitto 브로커로 연결
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true); // 클린 세션 사용
            mqttClient.connect(options);

            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(1); // QoS 설정

            // Mosquitto 브로커로 발행
            mqttClient.publish(topic, mqttMessage);
            System.out.printf("발행: [%s] -> %s%n", topic, message);
            mqttClient.disconnect();
        } catch (MqttException e) {
            System.err.println("메시지 발행 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 가공된 메시지 구조를 위한 클래스
    static class ProcessedMessage {
        private final double temperature;
        private final long timestamp;
        private final String tag;

        public ProcessedMessage(double temperature, long timestamp, String tag) {
            this.temperature = temperature;
            this.timestamp = timestamp;
            this.tag = tag;
        }

        public double getTemperature() {
            return temperature;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getTag() {
            return tag;
        }
    }
}
