package com.iot.mqtt;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MqttToDB implements Runnable {

    // 타임스탬프를 읽기 좋은 형식으로 변환하는 메서드
    private String convertTimestamp(long timestampMs) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(timestampMs));
    }

    public void run() {
        try {
            try (MqttClient client = new MqttClient("tcp://localhost:1883", "songsong")) {
                client.connect();

                client.setCallback(new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable cause) {
                        System.out.println(cause.toString());
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {

                        if (topic.contains("lora") || topic.contains("power_meter")) {
                            return;
                        }
                        Connection conn = null;

                        try {
                            // MQTT 메시지 페이로드를 JSON으로 파싱
                            String payload = new String(message.getPayload());
                            JsonNode jsonNode = new ObjectMapper().readTree(payload);

                            // time과 value 값을 추출
                            String name = jsonNode.get("payload").asText();
                            Long timeMillis = jsonNode.get("time").asLong();
                            String time = convertTimestamp(timeMillis);
                            double value = jsonNode.get("value").asDouble();

                            Class.forName("com.mysql.cj.jdbc.Driver");
                            String url = "jdbc:mysql://localhost:3306/IOT";
                            conn = DriverManager.getConnection(url, "root", "P@ssw0rd");

                            String insertSQL = "INSERT INTO modbus_test (name, value, timestamp) VALUES (?, ?, ?)";
                            PreparedStatement pstmt_modbus = conn.prepareStatement(insertSQL);
                            pstmt_modbus.setString(1, name);
                            pstmt_modbus.setDouble(2, value);
                            pstmt_modbus.setString(3, time);
                            pstmt_modbus.executeUpdate();

                            System.out.println("insert success");

                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        } finally {
                            if (conn != null) {
                                try {
                                    conn.close();
                                } catch (Exception ex) {
                                    System.out.println("Database connection close error: " + ex.getMessage());
                                }
                            }
                        }
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {
                    }
                });

                client.subscribe("songs/#");
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
