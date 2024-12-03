package com.nhnacademy.mqtt;

import java.time.Instant;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MqttBrokers {
    // MQTT 브로커 주소
    private static final String BROKER = "tcp://192.168.71.202:1883";
    private static final String CLIENT_ID = "atgn002"; // 클라이언트 ID
    private static final String TOPIC = "application/#"; // 구독 및 발행 주제

    private static final String INFLUXDB_URL = "http://192.168.71.202:8086";
    private static final String INFLUXDB_TOKEN = "b7KKn-OWOYSt7FwtqZBPRSVWg5qaHJOSNUYMO8t3CO0A38hzEnUCsAzS7ADO8NhcA6EVp44F4Yh-nm5lt40ZZw==";
    private static final String INFLUXDB_ORG = "root";
    private static final String INFLUXDB_BUCKET = "test";

    public static void main(String[] args) throws InterruptedException {

        try (InfluxDBClient influxDBClient = InfluxDBClientFactory.create(INFLUXDB_URL, INFLUXDB_TOKEN.toCharArray(),
                INFLUXDB_ORG, INFLUXDB_BUCKET);
                MqttClient client = new MqttClient(BROKER, CLIENT_ID)) {

            // 연결 설정
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true); // 클린 세션 사용
            // options.setAutomaticReconnect(true); // 자동 재연결 설정
            // options.setKeepAliveInterval(60); // 60초마다 PING 메시지를 보내 연결 유지

            // 메시지 수신 콜백 설정
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Connection lost: " + cause.getMessage());
                    cause.printStackTrace(); // 예외 스택 트레이스 출력
                    // try {
                    // client.connect(options); // 자동 재연결
                    // System.out.println("Reconnected to broker!");
                    // } catch (MqttException e) {
                    // e.printStackTrace();
                    // }
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // mqtt 변환
                    System.out.println("Received message from topic: " + topic);
                    /**
                     * String은 불변이지만 속도가 느림. 보통 짧은 문자를 더할 경우 사용
                     * 이 경우 StringBuilder를 이용하면 동기화 여부와 관계없이 빠르게 돼서 사용
                     */
                    StringBuilder payloadSB = new StringBuilder();
                    payloadSB.append(new String(message.getPayload()));
                    String payload = payloadSB.toString();
                    // String payload = new String(message.getPayload()); // 이 경우 속도가 너무 느리다.
                    // System.out.println("Message: " + payload);

                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode jsonMessage = objectMapper.readTree(payload);

                    // 필요한 정보 추출
                    JsonNode object = jsonMessage.get("object");
                    JsonNode deviceInfo = jsonMessage.get("deviceInfo");

                    if (object != null && deviceInfo != null) {
                        // InfluxDB에 쓸 데이터 준비
                        Point point = Point.measurement("device_data") // Measurement 이름 설정
                                .addTag("deviceName", deviceInfo.get("deviceName").asText())
                                .addTag("place", deviceInfo.get("tags").get("place").asText())
                                .addTag("branch", deviceInfo.get("tags").get("branch").asText())
                                .addTag("name", deviceInfo.get("tags").get("name").asText())
                                .time(Instant.now(), WritePrecision.MS); // 현재 시간 사용

                        // 필드들 (필드가 존재할 경우에만 추가)
                        if (object.has("humidity") && object.get("humidity").isNumber()) {
                            double humidity = object.get("humidity").asDouble();
                            point.addField("humidity", humidity);
                            System.out.println("humidity: " + humidity);
                        }
                        if (object.has("battery") && object.get("battery").isNumber()) {
                            int battery = object.get("battery").asInt();
                            point.addField("battery", battery);
                            System.out.println("battery: " + battery);
                        }
                        if (object.has("battery_level") && object.get("battery_level").isNumber()) {
                            int batteryLevel = object.get("battery_level").asInt();
                            point.addField("battery_level", batteryLevel);
                            System.out.println("battery_level: " + batteryLevel);
                        }
                        if (object.has("co2") && object.get("co2").isNumber()) {
                            int co2 = object.get("co2").asInt();
                            point.addField("co2", co2);
                            System.out.println("co2: " + co2);
                        }
                        if (object.has("distance") && object.get("distance").isNumber()) {
                            double distance = object.get("distance").asDouble();
                            point.addField("distance", distance);
                            System.out.println("distance: " + distance);
                        }
                        if (object.has("illumination") && object.get("illumination").isNumber()) {
                            double illumination = object.get("illumination").asDouble();
                            point.addField("illumination", illumination);
                            System.out.println("illumination: " + illumination);
                        }
                        if (object.has("infrared") && object.get("infrared").isNumber()) {
                            double infrared = object.get("infrared").asDouble();
                            point.addField("infrared", infrared);
                            System.out.println("infrared: " + infrared);
                        }
                        if (object.has("pressure") && object.get("pressure").isNumber()) {
                            double pressure = object.get("pressure").asDouble();
                            point.addField("pressure", pressure);
                            System.out.println("pressure: " + pressure);
                        }
                        if (object.has("temperature") && object.get("temperature").isNumber()) {
                            double temperature = object.get("temperature").asDouble();
                            point.addField("temperature", temperature);
                            System.out.println("temperature: " + temperature);
                        }
                        if (object.has("tvoc") && object.get("tvoc").isNumber()) {
                            double tvoc = object.get("tvoc").asDouble();
                            point.addField("tvoc", tvoc);
                            System.out.println("tvoc: " + tvoc);
                        }
                        if (object.has("activity") && object.get("activity").isNumber()) {
                            double activity = object.get("activity").asDouble();
                            point.addField("activity", activity);
                            System.out.println("activity: " + activity);
                        }

                        // InfluxDB에 데이터 기록
                        influxDBClient.getWriteApiBlocking().writePoint(point);
                        System.out.println("Data written to InfluxDB: " + point);
                    } else {
                        System.out.println("deviceName is missing or object is null");
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("Message delivery complete: " + token.getMessageId());
                }
            });

            // 브로커 연결
            System.out.println("Connecting to broker..." + BROKER);
            client.connect(options);
            System.out.println("Connected!");

            // 주제 구독
            System.out.println("Subscribing to topic: " + TOPIC);
            client.subscribe(TOPIC);

            // 메시지 발행
            // String message = "Hello, MQTT from Java!";
            // System.out.println("Publishing message: " + message);
            // client.publish(TOPIC, new MqttMessage(message.getBytes()));

            // 10초 대기 후 종료
            Thread.sleep(10_0000);

            // 클라이언트 종료
            System.out.println("Disconnecting...");
            client.disconnect();
            System.out.println("Disconnected!");

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
