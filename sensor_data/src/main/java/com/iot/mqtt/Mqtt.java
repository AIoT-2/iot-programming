package com.iot.mqtt;

import java.time.Instant;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.write.Point;
import com.influxdb.client.domain.WritePrecision;

public class Mqtt extends Mqtt_transform {
    // MQTT 브로커 주소
    private static final String BROKER = "tcp://192.168.70.203:1883";
    // 클라이언트 ID
    private static final String CLIENT_ID = "song";
    // 구독 및 발행 주제
    private static final String TOPIC = "data/#";

    // InfluxDB 설정
    private static final String INFLUXDB_URL = "http://192.168.71.207:8086"; // URL
    private static final String INFLUXDB_TOKEN = "aCden7eIjcqbw504Yp7gzHJsdozMJS9E-HqOlm6dKPCoQyp60OWVohL-ctZgFlkgMDiGWAaRLma5oQahCkIPiA=="; // token

    private static final String INFLUXDB_ORG = "123123"; // 조직
    private static final String INFLUXDB_BUCKET = "mqtt_data"; // 버킷

    public static void main(String[] args) throws InterruptedException {
        InfluxDBClient influxDBClient = InfluxDBClientFactory.create(INFLUXDB_URL, INFLUXDB_TOKEN.toCharArray(),
                INFLUXDB_ORG, INFLUXDB_BUCKET);

        try (MqttClient client = new MqttClient(BROKER, CLIENT_ID)) {

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            // 메시지 수신 콜백 설정
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Connection lost: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {

                    String payload = new String(message.getPayload());
                    ObjectMapper objectMapper = new ObjectMapper();
                    String measurement = extractMeasurement(topic);

                    try {
                        JsonNode jsonNode = objectMapper.readTree(payload);

                        Point pointBuilder = Point.measurement(measurement)
                                .addTag("spot", extractField(topic))
                                .addTag("value", extractValue(topic))
                                .addField("payload", jsonNode.toString())
                                .time(Instant.now(), WritePrecision.NS);

                        JsonNode valueNode = jsonNode.get("value");

                        if (valueNode != null && valueNode.isNumber()) {
                            pointBuilder.addField("value", valueNode.asDouble());
                        } else {
                            System.out.println("Skipping non-numeric value: " + valueNode);
                        }

                        influxDBClient.getWriteApiBlocking().writePoint(pointBuilder);

                        System.out.println("Field : " + extractField(topic));
                        System.out.println("Measurement : " + extractMeasurement(topic));
                        System.out.println("Value : " + extractValue(topic));
                        System.out.println("Topic: " + topic);
                        System.out.println("msg: " + jsonNode.toString());
                        System.out.println();
                    } catch (Exception e) {
                        System.out.println("Invalid JSON payload: " + payload);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("Message delivery complete: "
                            + token.getMessageId());
                }
            });

            System.out.println("Connecting to broker...");
            client.connect(options);
            System.out.println("Connected!");

            System.out.println("Subscribing to topic: " + TOPIC);
            client.subscribe(TOPIC);

            Thread.sleep(100000);

            System.out.println("Disconnecting...");
            client.disconnect();
            System.out.println("Disconnected!");

        } catch (MqttException e) {
            e.printStackTrace();
        } finally {
            influxDBClient.close();
        }
    }
}
