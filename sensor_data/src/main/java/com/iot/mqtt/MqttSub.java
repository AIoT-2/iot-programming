package com.iot.mqtt;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MqttSub extends MqttTransform implements Runnable {
    static final Logger logger = LoggerFactory.getLogger(MqttSub.class);

    private static final String BROKER = "tcp://localhost:1883";
    private static final String CLIENT_ID = "songs";
    private static final String TOPIC = "data/#";
    private static final int RECONNECT_INTERVAL = 10000; // 10초 간격으로 재연결 시도

    private MqttClient client;

    @Override
    public void run() {
        try {
            client = new MqttClient(BROKER, CLIENT_ID);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setKeepAliveInterval(60);
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    logger.error("Connection lost: {}", cause.getMessage());
                    reconnect();
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    logger.info("START messageArrived");
                    if (topic.contains("power_meter") || topic.contains("lora")) {
                        return;
                    }

                    String payload = new String(message.getPayload());
                    ObjectMapper objectMapper = new ObjectMapper();

                    try {
                        JsonNode jsonNode = objectMapper.readTree(payload);
                        TopicInfo topicInfo = parseTopicInfo(topic);

                        if (topicInfo != null) {
                            saveToDatabase(topicInfo, jsonNode);
                        }

                    } catch (Exception e) {
                        logger.error("Error processing message: ", e);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            connectAndSubscribe();

            while (client.isConnected()) {
                Thread.sleep(5000);
                logger.info("-ing");
            }

        } catch (MqttException | InterruptedException e) {
            logger.error("Error in MQTT client: {}", e.getMessage());
        } finally {
            disconnect();
        }
    }

    private void connectAndSubscribe() {
        try {
            client.connect();
            logger.info("Connected to MQTT broker: " + BROKER);

            if (client.isConnected()) {
                client.subscribe(TOPIC);
                logger.info("Successfully subscribed to topic: " + TOPIC);
            } else {
                logger.error("MQTT client failed to connect to broker");
            }
        } catch (MqttException e) {
            logger.error("Error connecting to MQTT broker: {}", e.getMessage());
            reconnect();
        }
    }

    private void reconnect() {
        while (!client.isConnected()) {
            try {
                logger.info("Attempting to reconnect to broker...");
                client.connect();
                client.subscribe(TOPIC); // 재연결 후 다시 구독
                logger.info("Reconnected and subscribed to topic: " + TOPIC);
                break; // 연결 성공 시 루프 종료
            } catch (MqttException e) {
                logger.error("Reconnection failed: {}", e.getMessage());
                try {
                    Thread.sleep(RECONNECT_INTERVAL); // 10초 간격으로 재연결 시도
                } catch (InterruptedException ie) {
                    logger.error("Reconnection attempt interrupted: {}", ie.getMessage());
                }
            }
        }
    }

    private void disconnect() {
        try {
            if (client.isConnected()) {
                client.disconnect();
                logger.info("Disconnected from broker.");
            }
        } catch (MqttException e) {
            logger.error("Error disconnecting from MQTT broker: {}", e.getMessage());
        }
    }

    private TopicInfo parseTopicInfo(String topic) {
        String[] parts = topic.split("/");
        if (parts.length < 10) {
            logger.warn("Invalid topic format: {}", topic);
            return null;
        }
        TopicInfo info = new TopicInfo();
        if (parts.length >= 15) {
            info.name = parts[15];
            info.place = parts[6];
            info.spot = parts[10];
        } else {
            info.name = parts[12];
            info.place = parts[6];
            info.spot = parts[10];
        }
        return info;
    }

    private void saveToDatabase(TopicInfo topicInfo, JsonNode jsonNode) {
        Connection conn = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://localhost:3306/IOT";
            conn = DriverManager.getConnection(url, "root", "P@ssw0rd");

            String insertSQL = "INSERT INTO mqtt_test (name, place, spot, value, timestamp) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(insertSQL);

            double value = jsonNode.get("value").asDouble();

            String timestamp = convertTimestamp(jsonNode.get("time").asLong());

            pstmt.setString(1, topicInfo.name);
            pstmt.setString(2, topicInfo.place);
            pstmt.setString(3, topicInfo.spot);
            pstmt.setDouble(4, value);
            pstmt.setString(5, timestamp);

            pstmt.executeUpdate();

            logger.info("데이터베이스에 성공적으로 데이터 저장: {} - {}", topicInfo.name, value);

        } catch (Exception e) {
            logger.error("데이터베이스 저장 중 오류 발생: ", e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception ex) {
                    logger.error("데이터베이스 연결 종료 중 오류 발생: ", ex);
                }
            }
        }
    }

    private String convertTimestamp(long timestampMs) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(timestampMs));
    }

    private static class TopicInfo {
        String name;
        String place;
        String spot;
    }

    public static void main(String[] args) {
        Thread mqttSubThread = new Thread(new MqttSub());
        mqttSubThread.start();
    }
}
