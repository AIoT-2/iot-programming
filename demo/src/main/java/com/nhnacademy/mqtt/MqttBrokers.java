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
import com.nhnacademy.settings.DemoSetting;

import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
public class MqttBrokers {
    public static void main(String[] args) throws InterruptedException {

        try (InfluxDBClient influxDBClient = InfluxDBClientFactory.create(DemoSetting.INFLUXDB_URL,
                DemoSetting.INFLUXDB_TOKEN.toCharArray(), DemoSetting.INFLUXDB_ORG, DemoSetting.INFLUXDB_BUCKET);
                MqttClient client = new MqttClient(DemoSetting.BROKER, DemoSetting.CLIENT_ID)) {

            // 연결 설정
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true); // 클린 세션 사용
            // options.setAutomaticReconnect(true); // 자동 재연결 설정
            // options.setKeepAliveInterval(60); // 60초마다 PING 메시지를 보내 연결 유지

            // 메시지 수신 콜백 설정
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.debug("Connection lost: {}", cause.getMessage());
                    // cause.printStackTrace(); // 예외 스택 트레이스 출력
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // mqtt 변환
                    log.debug("Received message from topic: {}", topic);
                    /**
                     * String은 불변이지만 속도가 느림. 보통 짧은 문자를 더할 경우 사용
                     * 이 경우 StringBuilder를 이용하면 동기화 여부와 관계없이 빠르게 돼서 사용
                     */
                    StringBuilder payloadSB = new StringBuilder();
                    payloadSB.append(new String(message.getPayload()));
                    String payload = payloadSB.toString();

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

                        String valueDouble = "double";
                        String valueInt = "int";

                        // 필드들 (필드가 존재할 경우에만 추가)
                        addFieldPresent(object, "humidity", point, "humidity", valueDouble);
                        addFieldPresent(object, "battery", point, "battery", valueInt);
                        addFieldPresent(object, "battery_level", point, "battery_level", valueInt);
                        addFieldPresent(object, "co2", point, "co2", valueInt);
                        addFieldPresent(object, "distance", point, "distance", valueDouble);
                        addFieldPresent(object, "illumination", point, "illumination", valueDouble);
                        addFieldPresent(object, "infrared", point, "infrared", valueDouble);
                        addFieldPresent(object, "pressure", point, "pressure", valueDouble);
                        addFieldPresent(object, "temperature", point, "temperature", valueDouble);
                        addFieldPresent(object, "tvoc", point, "tvoc", valueDouble);
                        addFieldPresent(object, "activity", point, "activity", valueDouble);

                        // InfluxDB에 데이터 기록
                        influxDBClient.getWriteApiBlocking().writePoint(point);
                        log.debug("Data written to InfluxDB: {}", point);
                    } else {
                        log.debug("deviceName is missing or object is null");
                    }
                }

                public void addFieldPresent(JsonNode object, String fieldName, Point point, String fieldKey,
                        String valueType) {
                    if (object.has(fieldName)) {
                        JsonNode fieldNode = object.get(fieldName);
                        if (valueType.equals("double") && fieldNode.isNumber()) {
                            point.addField(fieldKey, fieldNode.asDouble());
                            log.debug("{}: {}", fieldKey, fieldNode.asDouble());
                        } else if (valueType.equals("int") && fieldNode.isNumber()) {
                            point.addField(fieldKey, fieldNode.asInt());
                            log.debug("{}: {}", fieldKey, fieldNode.asInt());
                        }
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("Message delivery complete: " + token.getMessageId());
                }
            });

            // 브로커 연결
            System.out.println("Connecting to broker..." + DemoSetting.BROKER);
            client.connect(options);
            System.out.println("Connected!");

            // 주제 구독
            System.out.println("Subscribing to topic: " + DemoSetting.MQTT_TOPIC);
            client.subscribe(DemoSetting.MQTT_TOPIC);

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
