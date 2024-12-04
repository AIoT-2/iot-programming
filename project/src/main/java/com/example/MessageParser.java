package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.logging.Logger;

public class MessageParser {
    private static final Logger logger = Logger.getLogger(MessageParser.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void parseSensorData(String jsonMessage) {
        try {
            JsonNode rootNode = mapper.readTree(jsonMessage);
            
            // deviceInfo 파싱
            JsonNode deviceInfo = rootNode.get("deviceInfo");
            if (deviceInfo != null) {
                System.out.println("=== Device Info ===");
                System.out.println("Device Name: " + getNodeTextSafely(deviceInfo, "deviceName"));
                
                JsonNode tags = deviceInfo.get("tags");
                if (tags != null) {
                    System.out.println("Location: " + getNodeTextSafely(tags, "place"));
                    System.out.println("Specific Location: " + getNodeTextSafely(tags, "name"));
                }
            }

            // object(센서 데이터) 파싱
            JsonNode sensorData = rootNode.get("object");
            if (sensorData != null) {
                System.out.println("\n=== Sensor Data ===");
                
                Double temp = getNodeDoubleSafely(sensorData, "temperature");
                if (temp != null) {
                    System.out.println("Temperature: " + temp + "°C");
                }
                
                Double humidity = getNodeDoubleSafely(sensorData, "humidity");
                if (humidity != null) {
                    System.out.println("Humidity: " + humidity + "%");
                }
                
                Double battery = getNodeDoubleSafely(sensorData, "battery");
                if (battery != null) {
                    System.out.println("Battery: " + battery + "%");
                }
            }
            
            System.out.println("==================\n");

        } catch (Exception e) {
            logger.warning("Error parsing JSON: " + e.getMessage());
        }
    }

    // 안전하게 텍스트 값을 가져오는 메소드
    private static String getNodeTextSafely(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        return fieldNode != null ? fieldNode.asText() : "N/A";
    }

    // 안전하게 Double 값을 가져오는 메소드
    private static Double getNodeDoubleSafely(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asDouble() : null;
    }
}