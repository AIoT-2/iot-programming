package com.nhnacademy.db;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;

import java.util.Iterator;
import java.util.Map;


@Slf4j
public class InfluxDB implements Runnable {
    private static final String MQTT_BROKER = "tcp://127.0.0.1:1883"; // MQTT 브로커 주소
    private static final String MQTT_TOPIC = "sensor/data";         // 구독할 MQTT 주제
    private static final String INFLUXDB_URL = "http://192.168.71.209:8086"; // InfluxDB 주소
    private static final String INFLUXDB_DATABASE = "sensor_data";      // 데이터베이스 이름
    private static final String INFLUXDB_MEASUREMENT = "sensor_metrics"; // 측정값 이름
    private static final String TOKENS = "LAJkxUibftEm_UsmYpBhXpjlUs63VcuGdyEgCTUO85dINuFtMCalOm4gMwNfAMBRRHmyQNTq546IqxheEQmUkA==";

    private static final String influxDBOrg = "nhnacademy"; // Organization 이름
    private static final String influxDBBucket = "test"; // 사용할 Bucket 이름

    private InfluxDBClient influxDB;

    /**
     * InfluxDB에 연결
     */
    private void connectToInfluxDB() {
        try {
            influxDB = InfluxDBClientFactory.create(INFLUXDB_URL,TOKENS.toCharArray(),influxDBOrg,influxDBBucket);
            log.info("Connected to InfluxDB.");
        } catch (Exception e) {
            log.info("Failed to connect to InfluxDB:{} ", e.getMessage());
        }
    }

    /**
     * MQTT 브로커에 연결 및 구독
     */
    private void subscribeToMqtt() {
        try {
            MqttClient mqttClient = new MqttClient(MQTT_BROKER, MqttClient.generateClientId());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                   log.info("Connection to MQTT broker lost: {}" , cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    log.info("Received message: {}" , payload);
                    handleIncomingData(payload);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Not used for subscribers
                }
            });

            mqttClient.connect(options);
            mqttClient.subscribe(MQTT_TOPIC);
            log.info("Subscribed to topic: {}" , MQTT_TOPIC);

        } catch (Exception e) {
            log.info("Failed to subscribe to MQTT: {} " ,e.getMessage());
        }
    }

    /**
     * MQTT 메시지 처리 및 InfluxDB에 데이터 저장
     */
    private void handleIncomingData(String payload) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode json = objectMapper.readTree(payload);  // payload를 JsonNode로 변환

            // 기본 태그 설정
            JsonNode tags = json.get("tags");
            String deviceName = tags.get("deviceId").asText();  // "deviceId" 값 추출
            String location = tags.get("location").asText();
            // 측정값 생성
            Point point = Point.measurement(INFLUXDB_MEASUREMENT)
                    .time(System.currentTimeMillis(), WritePrecision.MS)
                    .addTag("deviceName", deviceName)
                    .addTag("location", location);

            // 동적으로 데이터 필드 추가
            JsonNode data = json.get("data");
            Iterator<Map.Entry<String, JsonNode>> fields = data.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String key = field.getKey();
                JsonNode valueNode = field.getValue();


                if (valueNode.isNumber()) {
                    // 숫자 값을 Double로 변환하여 필드에 추가
                    point.addField(key, valueNode.asDouble());
                } else if (valueNode.isBoolean()) {
                    // Boolean 값을 필드에 추가
                    point.addField(key, valueNode.asBoolean());
                } else if (valueNode.isTextual()) {
                    // String 값을 필드에 추가
                    point.addField(key, valueNode.asText());
                } else {
                    System.err.println("Unsupported field type for key: " + key + ", value: " + valueNode);
                }
            }

            // InfluxDB에 데이터 쓰기
            influxDB.getWriteApiBlocking().writePoint(point);
            log.info("Data written to InfluxDB:{} ",  point);

        } catch (Exception e) {
            log.info("Failed to process message:{} ", e.getMessage());
        }
    }

    @Override
    public void run() {
        connectToInfluxDB();
        subscribeToMqtt();
    }
}
