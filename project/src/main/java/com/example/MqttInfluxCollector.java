package com.example;

import java.time.Instant;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;

public class MqttInfluxCollector {
    private static final Logger logger = Logger.getLogger(MqttInfluxCollector.class.getName());
    private static final String MQTT_BROKER = "tcp://localhost:1883";
    private static final String MQTT_CLIENT_ID = "hoho";

    // InfluxDB 설정
    private static final String INFLUX_URL = "http://192.168.71.226:8086";
    private static final String INFLUX_TOKEN = "3ljVvVjdI2mPLz3UI4SR7BPm2_11FT6aqGzaTNDNNR0xCS6dvqSidjQBL7M8CkzcaMaRyK7unUnd4SpVEPuigQ==";
    private static final String INFLUX_ORG = "myorg";
    private static final String INFLUX_BUCKET = "mybucket";

    private final MqttClient mqttClient;
    private final InfluxDBClient influxDBClient;
    private final WriteApiBlocking writeApi;
    private final ObjectMapper mapper = new ObjectMapper();

    public MqttInfluxCollector() throws MqttException {
        try {
            // MQTT 클라이언트 설정
            this.mqttClient = new MqttClient(MQTT_BROKER, MQTT_CLIENT_ID);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            mqttClient.connect(options);
            logger.info("Connected to MQTT broker: " + MQTT_BROKER);

            // InfluxDB 클라이언트 설정
            this.influxDBClient = InfluxDBClientFactory.create(INFLUX_URL,
                    INFLUX_TOKEN.toCharArray(),
                    INFLUX_ORG,
                    INFLUX_BUCKET);
            this.writeApi = influxDBClient.getWriteApiBlocking();

            // InfluxDB 연결 테스트
            try {
                influxDBClient.ping();
                logger.info("Successfully connected to InfluxDB: " + INFLUX_URL);
            } catch (Exception e) {
                logger.severe("Failed to connect to InfluxDB: " + e.getMessage());
                throw e;
            }

            setupMqttCallback();
        } catch (Exception e) {
            logger.severe("Error in constructor: " + e.getMessage());
            throw new MqttException(e.getCause());
        }
    }

    private void setupMqttCallback() {
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                logger.warning("Connection to MQTT broker lost: " + cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String payload = new String(message.getPayload());
                logger.info("Received message - Topic: " + topic);
                logger.info("Payload: " + payload);

                try {
                    if (topic.startsWith("power/")) {
                        processPowerData(payload);
                    } else if (topic.startsWith("sensor/")) {
                        processSensorData(payload);
                    }
                } catch (Exception e) {
                    logger.severe("Error processing message: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // 메시지 전송 완료 처리 (구독자는 사용하지 않음)
            }
        });
    }

    private void processPowerData(String payload) {
        try {
            JsonNode data = mapper.readTree(payload);

            Point point = Point.measurement("power_metrics")
                    .time(data.has("timestamp") ? data.get("timestamp").asLong() : System.currentTimeMillis(),
                            WritePrecision.MS)
                    .addTag("deviceName", data.get("location").asText());

            // 모든 필드를 동적으로 추가
            data.fields().forEachRemaining(field -> {
                String key = field.getKey();
                JsonNode value = field.getValue();

                // location과 timestamp는 이미 처리했으므로 제외
                if (!key.equals("location") && !key.equals("timestamp")) {
                    if (value.isNumber()) {
                        point.addField(key, value.asDouble());
                    } else if (value.isBoolean()) {
                        point.addField(key, value.asBoolean());
                    } else if (value.isTextual()) {
                        point.addField(key, value.asText());
                    }
                }
            });

            writeApi.writePoint(point);
            logger.info("Power data written to InfluxDB for device: " +
                    data.get("location").asText());

        } catch (Exception e) {
            logger.severe("Error processing power data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processSensorData(String payload) {
        try {
            JsonNode data = mapper.readTree(payload);
            JsonNode deviceInfo = data.get("deviceInfo");
            JsonNode object = data.get("object");

            if (deviceInfo == null || object == null) {
                logger.warning("Missing required sensor data");
                return;
            }

            Point point = Point.measurement("sensor_metrics")
                    .time(Instant.parse(data.get("time").asText()), WritePrecision.MS)
                    .addTag("deviceName", deviceInfo.get("deviceName").asText());

            // object의 모든 필드를 동적으로 추가
            object.fields().forEachRemaining(field -> {
                String key = field.getKey();
                JsonNode value = field.getValue();

                if (value.isNumber()) {
                    point.addField(key, value.asDouble());
                } else if (value.isBoolean()) {
                    point.addField(key, value.asBoolean());
                } else if (value.isTextual()) {
                    point.addField(key, value.asText());
                }
            });

            writeApi.writePoint(point);
            logger.info("Sensor data written to InfluxDB for device: " +
                    deviceInfo.get("deviceName").asText());

        } catch (Exception e) {
            logger.severe("Error processing sensor data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void startCollecting() throws MqttException {
        mqttClient.subscribe("power/#");
        mqttClient.subscribe("sensor/#");
        logger.info("Started collecting data from MQTT topics: power/#, sensor/#");
    }

    public void close() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                logger.info("Disconnected from MQTT broker");
            }
            if (influxDBClient != null) {
                influxDBClient.close();
                logger.info("Closed InfluxDB connection");
            }
        } catch (Exception e) {
            logger.severe("Error closing connections: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    public static void main(String[] args) {
        while (true) { // 무한 루프로 프로그램 계속 실행
            MqttInfluxCollector collector = null;
            try {
                collector = new MqttInfluxCollector();
                collector.startCollecting();

                // 종료 처리
                final MqttInfluxCollector finalCollector = collector;
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    if (finalCollector != null) {
                        finalCollector.close();
                    }
                }));

                // 연결 유지 확인
                while (true) {
                    Thread.sleep(5000);
                    if (!collector.isConnected()) {
                        throw new Exception("Connection lost, attempting to reconnect...");
                    }
                }

            } catch (Exception e) {
                logger.severe("Error occurred: " + e.getMessage());
                e.printStackTrace();

                if (collector != null) {
                    collector.close();
                }

                try {
                    logger.info("Waiting 5 seconds before reconnecting...");
                    Thread.sleep(5000); // 5초 대기 후 재시도
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                // continue를 통해 while 루프의 처음으로 돌아가 재시도
                continue;
            }
        }
    }
}