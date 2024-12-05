package com.nhnacademy;

import org.eclipse.paho.client.mqttv3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;

import java.time.Instant;

public class TemperatureToDB {
    // MQTT 브로커 주소
    private static final String BROKER = "tcp://192.168.70.203:1883";
    // 클라이언트 ID
    private static final String CLIENT_ID = "MyJavaClientExample";
    // 구독 및 발행 주제
    private static final String TOPIC = "application/#";
    
    public static void main(String[] args) {
        InfluxDBHandler influxDBHandler = new InfluxDBHandler();

        try (MqttClient client = new MqttClient(BROKER, CLIENT_ID)) {
            // 연결 설정
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true); // 클린 세션 사용

            // 메시지 수신 콜백 설정
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.err.println("MQTT 연결이 끊여졌습니다." + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // 수신한 메시지 출력
                    String payload = new String(message.getPayload());

                    // JSON 파싱
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode rootNode = mapper.readTree(payload);
                    if (rootNode.has("object") && rootNode.get("object").has("temperature")) {
                        double temperature = rootNode.get("object").get("temperature").asDouble();
                        long timestamp = System.currentTimeMillis();
                        String deviceName = rootNode.get("deviceInfo").get("deviceName").asText();
                        // 토픽 설정
                        final String temperatureTopic = "sensor/temperature";

                        // 가공된 메시지 생성
                        String processedPayload = mapper.writeValueAsString(new ProcessedMessage(
                            temperature,
                            timestamp,
                            deviceName
                        ));

                        System.out.printf("%s : %s %n", temperatureTopic, processedPayload);

                        // InfluxDB Point 생성 (InfluxDB 2.x 형식)
                        Instant instant = Instant.ofEpochMilli(timestamp); // long을 Instant로 변환
                        Point point = Point.measurement("java_influx")
                                .addTag("deviceName", deviceName)  // `tag()` 대신 `addTag()` 사용
                                .addField("temperature", temperature)
                                .time(instant, WritePrecision.MS); // time() 메서드에서 Instant와 WritePrecision 사용

                        // InfluxDB에 데이터 쓰기
                        influxDBHandler.writeData(point);
                        
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

            // 브로커 연결
            client.connect(options);

            // 주제 구독
            client.subscribe(TOPIC);
            System.out.println("MQTT 브로커에 연결되고 구독을 시작했습니다.");

            // 10초 대기 후 종료
            Thread.sleep(100000);

            // 클라이언트 종료
            if (client.isConnected()) {
                try {
                    System.out.println("Disconnecting...");
                    client.disconnect();
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

}
