package com.iot.mqtt;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MqttToDB implements Runnable {
    public void run() {
        try {
            try (MqttClient client = new MqttClient("tcp://192.168.70.203:1883", "songsong")) {
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
                            String timeMillis = jsonNode.get("time").asText();
                            double value = jsonNode.get("value").asDouble();

                            Class.forName("com.mysql.cj.jdbc.Driver");
                            String url = "jdbc:mysql://localhost:3306/TEST";
                            String sql = "insert into new_table values(?, ?, ?)";
                            conn = DriverManager.getConnection(url, "root", "P@ssw0rd");
                            PreparedStatement pstmt = conn.prepareStatement(sql);
                            pstmt.setString(1, name);
                            pstmt.setDouble(2, value);
                            pstmt.setString(3, timeMillis);
                            pstmt.executeUpdate();

                        } catch (Exception e) {
                            System.out.println(e.getMessage());
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