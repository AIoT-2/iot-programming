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
    private static final String MQTT_CLIENT_ID = "InfluxCollector";
    
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
        // MQTT 클라이언트 설정
        this.mqttClient = new MqttClient(MQTT_BROKER, MQTT_CLIENT_ID);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        mqttClient.connect(options);

        // InfluxDB 클라이언트 설정
        this.influxDBClient = InfluxDBClientFactory.create(INFLUX_URL, 
            INFLUX_TOKEN.toCharArray(),
            INFLUX_ORG, 
            INFLUX_BUCKET);
        this.writeApi = influxDBClient.getWriteApiBlocking();

        setupMqttCallback();
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
                
                if (topic.startsWith("power/")) {
                    processPowerData(payload);
                } else if (topic.startsWith("sensor/")) {
                    processSensorData(payload);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });
    }

    private void processPowerData(String payload) {
        try {
            JsonNode data = mapper.readTree(payload);
            
            Point point = Point.measurement("power")
                .addTag("location", data.get("location").asText())
                .addTag("deviceType", data.get("deviceType").asText())
                .addField("powerW", data.get("powerW").asDouble())
                .addField("powerVA", data.get("powerVA").asDouble())
                .addField("powerVAR", data.get("powerVAR").asDouble())
                .addField("voltage", data.get("voltage").asDouble())
                .addField("current", data.get("current").asDouble())
                .addField("powerFactor", data.get("powerFactor").asDouble())
                .addField("currentTHD", data.get("currentTHD").asDouble())
                .addField("currentUnbalance", data.get("currentUnbalance").asDouble())
                .time(Instant.now(), WritePrecision.NS);

            writeApi.writePoint(point);
            logger.info("Power data written to InfluxDB");

        } catch (Exception e) {
            logger.warning("Error processing power data: " + e.getMessage());
            e.printStackTrace();
        }
    }

   private void processSensorData(String payload) {
    try {
        JsonNode data = mapper.readTree(payload);
        JsonNode object = data.get("object");
        JsonNode deviceInfo = data.get("deviceInfo");
        
        if (object != null && deviceInfo != null) {
            Point point = Point.measurement("sensor")
                .addTag("deviceName", deviceInfo.get("deviceName").asText())
                .addTag("deviceType", deviceInfo.get("deviceProfileName").asText())
                .addTag("location", deviceInfo.get("tags").get("place").asText())
                .time(Instant.now(), WritePrecision.NS);  // 시간 추가
            
            // 모든 센서 필드 추가
            object.fields().forEachRemaining(field -> {
                String key = field.getKey();
                JsonNode value = field.getValue();
                if (value.isNumber()) {
                    point.addField(key, value.asDouble());
                } else {
                    point.addField(key, value.asText());
                }
            });

            writeApi.writePoint(point);  // build() 제거
            logger.info("Sensor data written to InfluxDB for device: " + 
                deviceInfo.get("deviceName").asText());
        }

    } catch (Exception e) {
        logger.warning("Error processing sensor data: " + e.getMessage());
        e.printStackTrace();
    }
}
    public void startCollecting() throws MqttException {
        mqttClient.subscribe("power/#");
        mqttClient.subscribe("sensor/#");
        logger.info("Started collecting data from MQTT topics");
    }

    public void close() {
        try {
            mqttClient.disconnect();
            influxDBClient.close();
        } catch (MqttException e) {
            logger.severe("Error closing connections: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            MqttInfluxCollector collector = new MqttInfluxCollector();
            collector.startCollecting();

            // 종료 처리
            Runtime.getRuntime().addShutdownHook(new Thread(collector::close));

            // 프로그램 계속 실행
            while (true) {
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            logger.severe("Error in main: " + e.getMessage());
            e.printStackTrace();
        }
    }
}