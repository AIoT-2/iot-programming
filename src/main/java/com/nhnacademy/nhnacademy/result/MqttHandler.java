package com.nhnacademy.nhnacademy.result;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import org.eclipse.paho.client.mqttv3.*;
import org.json.JSONObject;

public class MqttHandler {

    private static final String MQTT_BROKER = "tcp://127.0.0.1:1883"; // MQTT 브로커 주소
    private static final String MQTT_TOPIC = "sensor/data";         // 구독할 MQTT 주제
    private static final String INFLUXDB_URL = "http://192.168.71.209:8086"; // InfluxDB 주소
    private static final String INFLUXDB_DATABASE = "sensor_data";      // 데이터베이스 이름
    private static final String INFLUXDB_MEASUREMENT = "sensor_metrics"; // 측정값 이름
    private static final String TOKENS = "LAJkxUibftEm_UsmYpBhXpjlUs63VcuGdyEgCTUO85dINuFtMCalOm4gMwNfAMBRRHmyQNTq546IqxheEQmUkA==";

    private static final String influxDBOrg = "nhnacademy"; // Organization 이름
    private static final String influxDBBucket = "test"; // 사용할 Bucket 이름

    private InfluxDBClient influxDB;

    public static void main(String[] args) {

        MqttHandler handler = new MqttHandler();
        handler.connectToInfluxDB();
        handler.subscribeToMqtt();
    }

    /**
     * InfluxDB에 연결
     */
    private void connectToInfluxDB() {
        try {
            influxDB = InfluxDBClientFactory.create(INFLUXDB_URL,TOKENS.toCharArray(),influxDBOrg,influxDBBucket);
            System.out.println("Connected to InfluxDB.");
        } catch (Exception e) {
            System.err.println("Failed to connect to InfluxDB: " + e.getMessage());
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
                    System.err.println("Connection to MQTT broker lost: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    System.out.println("Received message: " + payload);
                    handleIncomingData(payload);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Not used for subscribers
                }
            });

            mqttClient.connect(options);
            mqttClient.subscribe(MQTT_TOPIC);
            System.out.println("Subscribed to topic: " + MQTT_TOPIC);

        } catch (Exception e) {
            System.err.println("Failed to subscribe to MQTT: " + e.getMessage());
        }
    }

    /**
     * MQTT 메시지 처리 및 InfluxDB에 데이터 저장
     */
    private void handleIncomingData(String payload) {
        try {
            JSONObject json = new JSONObject(payload);
            JSONObject data = json.getJSONObject("data");
            String deviceName = json.getString("deviceName");

            // InfluxDB에 데이터 작성
            Point point = Point.measurement(INFLUXDB_MEASUREMENT)
                    .time(System.currentTimeMillis(), WritePrecision.MS)
                    .addTag("deviceName", deviceName)
                    .addField("type", data.getInt("type"))
                    .addField("currentUnbalance4", data.getDouble("currentUnbalance4"))
                    .addField("ITHDAverage", data.getDouble("ITHDAverage"))
                    .addField("voltUnbalance", data.getDouble("voltUnbalance"))
                    .addField("pfAverage", data.getDouble("pfAverage"))
                    .addField("W", data.getLong("W"))
                    .addField("I1", data.getDouble("I1"))
                    .addField("V1", data.getDouble("V1"))
                    .addField("phase", data.getDouble("phase"))
                    .addField("IGR", data.getDouble("IGR"))
                    .addField("leakageCurrent", data.getDouble("leakageCurrent"));

            influxDB.getWriteApiBlocking().writePoint(point);
            System.out.println("Data written to InfluxDB: " + point);

        } catch (Exception e) {
            System.err.println("Failed to process message: " + e.getMessage());
        }
    }
}
