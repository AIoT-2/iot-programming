package com.nhnacademy.application;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.nhnacademy.modbus.ConfigurationData;
import com.nhnacademy.modbus.MasterTCP;
import com.nhnacademy.mqtt.MqttBrokers;
import com.nhnacademy.mqttpub.MqttPub;
import com.nhnacademy.settings.DemoSetting;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MainApp {
    public static void main(String[] args) throws InterruptedException {
        try (InfluxDBClient influxDBClient = InfluxDBClientFactory.create(DemoSetting.INFLUXDB_URL,
                DemoSetting.INFLUXDB_TOKEN.toCharArray(), DemoSetting.INFLUXDB_ORG, DemoSetting.INFLUXDB_BUCKET)) {

            // MqttPub 인스턴스 생성
            MqttPub mqttPub = new MqttPub(influxDBClient, DemoSetting.BROKER);

            // MQTT 브로커 사용 여부 설정
            boolean useMqttBrokers = false; // true -> MQTT 메시지 수신, false -> Modbus TCP 처리

            if (useMqttBrokers) {
                // MQTT 브로커로부터 메시지 수신
                MqttBrokers.startListening(DemoSetting.BROKER, DemoSetting.MQTT_TOPIC, mqttPub);
            } else {
                // Modbus 데이터 읽기 및 MQTT 발행
                MasterTCP masterTCP = new MasterTCP();
                int slaveId = 1;
                int offset = 100;
                int quantity = 32;
                while (offset <= 2400) {
                    Map<String, Object> modbusData = masterTCP.readModbusData(slaveId, offset, quantity);

                    Map<Integer, String> topicMap = ConfigurationData.topicMapName();
                    String topic = topicMap.get(offset);

                    // JSON으로 변환 후 MQTT로 발행
                    String jsonPayload = new ObjectMapper().writeValueAsString(modbusData);
                    mqttPub.publishJsonMessage(topic, jsonPayload);

                    // InfluxDB에 기록
                    JsonNode object = new ObjectMapper().readTree(jsonPayload);
                    JsonNode measurements = object.get("measurements");
                    mqttPub.writeModbusToInfluxDB(object, measurements);

                    offset += 100;
                }
            }

            // 1000초 대기 후 종료
            Thread.sleep(1000000);

            // 연결 종료
            mqttPub.disconnect();
            MqttBrokers.stopListening();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
