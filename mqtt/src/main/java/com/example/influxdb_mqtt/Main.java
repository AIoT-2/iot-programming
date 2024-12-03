package com.example.influxdb_mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import org.eclipse.paho.client.mqttv3.*;

import java.time.Instant;

public class Main {
    private static final String BROKER = "tcp://192.168.70.203:1883";
    private static final String CLIENT_ID = "JavaClientExample";
    private static final String TOPIC = "data/#";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws InterruptedException {
        InfluxDBConnector.initialize(); // InfluxDB 초기화

        try (MqttClient client = new MqttClient(BROKER, CLIENT_ID)) {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Connection lost: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    handleMessage(topic, payload);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("Message delivery complete: " + token.getMessageId());
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
            InfluxDBConnector.close(); // InfluxDB 연결 닫기
        }
    }

    private static void handleMessage(String topic, String payload) {
        try {
            // 메시지 데이터 처리
            String deviceName = extractDeviceName(topic);
            if (topic.contains("temperature")) {
                double temperature = extractValueFromPayload(payload, "value");
                System.out.println("[" + deviceName + "] Temperature: " + temperature);

                // InfluxDB에 데이터 저장
                InfluxDBConnector.writeData("temperature", "value", temperature);
            }

        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
        }
    }

    private static String extractDeviceName(String topic) {
        String[] parts = topic.split("/");
        return parts.length > 6 ? parts[6] : "UnknownDevice";
    }

    private static double extractValueFromPayload(String payload, String key) {
        try {
            JsonNode rootNode = objectMapper.readTree(payload);
            return rootNode.has(key) ? rootNode.get(key).asDouble() : Double.NaN;
        } catch (Exception e) {
            System.err.println("Error parsing JSON payload: " + e.getMessage());
            return Double.NaN;
        }
    }
}
