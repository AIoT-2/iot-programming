package com.nhnacademy.mqttpub;

import java.time.Instant;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.fasterxml.jackson.databind.JsonNode;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.nhnacademy.settings.DemoSetting;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MqttPub {
    private MqttClient mqttClient;
    private InfluxDBClient influxDBClient;
    private String valueDouble = "double";
    private String valueInt = "int";

    // 생성자
    public MqttPub(InfluxDBClient influxDBClient, String brokerUrl) {
        this.influxDBClient = influxDBClient;
        try {
            // MqttClient 객체 초기화
            mqttClient = new MqttClient(brokerUrl, DemoSetting.CLIENT_ID);

            // MqttClient 연결 옵션 설정
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            // MQTT 브로커에 연결
            mqttClient.connect(options);
            log.info("Connected to MQTT broker at: " + brokerUrl);
        } catch (MqttException e) {
            log.error("Failed to connect to MQTT broker: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // MQTT 브로커에 연결하는 메서드
    public void connectToBroker(String brokerUrl) throws MqttException {
        mqttClient = new MqttClient(brokerUrl, MqttClient.generateClientId());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        mqttClient.connect(options);
        System.out.println("Connected to " + brokerUrl);
    }

    // MQTT 메시지 발행, modbus 전용
    public void publishJsonMessage(String topic, String message) {
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttClient.publish(topic, mqttMessage);
            log.debug("Published message to topic: " + topic + ", Message: " + message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // InfluxDB에 기록
    public void writeToInfluxDB(JsonNode object, JsonNode deviceInfo) {
        try {
            Point point = Point.measurement("device_data")
                    .addTag("deviceName", deviceInfo.get("deviceName").asText())
                    .addTag("place", deviceInfo.get("tags").get("place").asText())
                    .addTag("branch", deviceInfo.get("tags").get("branch").asText())
                    .addTag("name", deviceInfo.get("tags").get("name").asText())
                    .time(Instant.now(), WritePrecision.MS);

            // 필드가 있을 경우, InfluxDB에 추가
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

            influxDBClient.getWriteApiBlocking().writePoint(point);
            log.info("Data written to InfluxDB: " + point);
        } catch (Exception e) {
            log.error("writeToInfluxDB error: {}", e.getMessage());
        }
    }

    public void writeModbusToInfluxDB(JsonNode object, JsonNode measurements) {
        try {
            Point point = Point.measurement("modbus_data") // 측정 이름 설정
                    .addTag("name", measurements.get("name").asText()) // 태그 추가
                    .time(Instant.now(), WritePrecision.MS); // 시간 태그 추가

            addFieldPresent(object, "operation Heartbit", point, "operation Heartbit", valueInt);
            addFieldPresent(object, "temperature", point, "temperature", valueInt);
            addFieldPresent(object, "frequency", point, "frequency", valueInt);
            addFieldPresent(object, "program version", point, "program version", valueInt);
            addFieldPresent(object, "present CO2 use(month)", point, "present CO2 use(month)", valueInt);
            addFieldPresent(object, "operation Heartbit 1", point, "operation Heartbit 1", valueInt);
            addFieldPresent(object, "frequency 1", point, "frequency 1", valueInt);
            addFieldPresent(object, "program version 1", point, "program version 1", valueInt);
            addFieldPresent(object, "present CO2 use(month) 1", point, "present CO2 use(month) 1", valueInt);
            addFieldPresent(object, "V123(LN) average", point, "V123(LN) average", valueInt);
            addFieldPresent(object, "V123(LL) average", point, "V123(LL) average", valueInt);
            addFieldPresent(object, "V1", point, "V1", valueInt);
            addFieldPresent(object, "V12", point, "V12", valueInt);
            addFieldPresent(object, "V1 unbalance", point, "V1 unbalance", valueInt);
            addFieldPresent(object, "V12 unbalance", point, "V12 unbalance", valueInt);
            addFieldPresent(object, "V2", point, "V2", valueInt);
            addFieldPresent(object, "V23", point, "V23", valueInt);
            addFieldPresent(object, "V2 unbalance", point, "V2 unbalance", valueInt);
            addFieldPresent(object, "V23 unbalance", point, "V23 unbalance", valueInt);
            addFieldPresent(object, "V3", point, "V3", valueInt);
            addFieldPresent(object, "V31", point, "V31", valueInt);
            addFieldPresent(object, "V3 unbalance", point, "V3 unbalance", valueInt);
            addFieldPresent(object, "V31 unbalance", point, "V31 unbalance", valueInt);
            addFieldPresent(object, "V1 THD", point, "V1 THD", valueInt);
            addFieldPresent(object, "V2 THD", point, "V2 THD", valueInt);
            addFieldPresent(object, "V3 THD", point, "V3 THD", valueInt);

            influxDBClient.getWriteApiBlocking().writePoint(point);
            log.info("Data written to InfluxDB: " + point);
        } catch (Exception e) {
            log.error("writeModbusToInfluxDB error: {}", e.getMessage());
        }
    }

    // 필드가 존재하면 InfluxDB에 추가, 파일이름 변경가능 버전
    private void addFieldPresent(JsonNode object, String fieldName, Point point, String fieldKey, String valueType) {
        if (object != null && object.has(fieldName)) {
            JsonNode fieldNode = object.get(fieldName);
            if (fieldNode.isNumber()) {
                if (valueType.equals("double")) {
                    point.addField(fieldKey, fieldNode.asDouble());
                    log.debug("{}: {}", fieldKey, fieldNode.asDouble());
                } else if (valueType.equals("int")) {
                    point.addField(fieldKey, fieldNode.asInt());
                    log.debug("{}: {}", fieldKey, fieldNode.asInt());
                }
            }
        }
    }

    // MQTT 클라이언트 연결 종료
    public void disconnect() throws MqttException {
        if (mqttClient != null && mqttClient.isConnected()) {
            mqttClient.disconnect();
            System.out.println("Disconnected from MQTT Broker.");
        }
    }
}
