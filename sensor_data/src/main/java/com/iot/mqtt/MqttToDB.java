package com.iot.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class MqttToDB {

    static final Logger logger = LoggerFactory.getLogger(MqttToDB.class);

    private static final String MQTT_BROKER = "tcp://localhost:1883";
    private static final String MQTT_CLIENT_ID = "songs";

    private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/test1";
    private static final String MYSQL_USER = "root";
    private static final String MYSQL_PASSWORD = "P@ssw0rd";

    private MqttClient mqttClient;
    private Connection mysqlConnection;

    public MqttToDB() {
        try {
            // Initialize MQTT client
            mqttClient = new MqttClient(MQTT_BROKER, MQTT_CLIENT_ID);
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    logger.error("MQTT connection lost", cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload = new String(message.getPayload());
                    logger.info("Received message: topic={}, payload={}", topic, payload);
                    insertIntoMySQL(topic, payload);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            // Initialize MySQL connection
            mysqlConnection = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD);
            logger.info("Connected to MySQL database");

        } catch (MqttException | SQLException e) {
            logger.error("Error initializing MqttToDB", e);
        }
    }

    void insertIntoMySQL(String topic, String payload) {
        String sql = "INSERT INTO test1 (topic, payload, timestamp) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = mysqlConnection.prepareStatement(sql)) {
            pstmt.setString(1, topic);
            pstmt.setString(2, payload);
            pstmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            pstmt.executeUpdate();
            logger.info("Data inserted into MySQL: topic={}, payload={}", topic, payload);
        } catch (SQLException e) {
            logger.error("Error inserting data into MySQL", e);
        }
    }

    public void close() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                mqttClient.close();
            }
            if (mysqlConnection != null && !mysqlConnection.isClosed()) {
                mysqlConnection.close();
            }
            logger.info("MQTT and MySQL connections closed.");
        } catch (MqttException | SQLException e) {
            logger.error("Error closing connections", e);
        }
    }
}
