package com.iot.modbus;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ModbusTransform {
    static final Logger logger = LoggerFactory.getLogger(ModbusTransform.class);

    public static List<Map<String, Object>> loadJsonData(String jsonFilePath) throws StreamReadException {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(new File(jsonFilePath), new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            System.out.println("Failed to load JSON file: " + e.getMessage());
            return null;
        }
    }

    public String buildPayload(String locationName, Map<String, Object> channelData, int registerAddress,
            double scaledValue) {
        // JSON 형식으로 변환할 객체를 생성
        Map<String, Object> payload = new HashMap<>();
        payload.put("p", locationName);
        payload.put("s", channelData);
        payload.put("r", registerAddress);
        payload.put("e", scaledValue);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // Map 객체를 JSON 문자열로 변환
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            logger.error("Error building JSON payload: " + e.getMessage(), e);
            return null; // 실패 시 null 반환
        }
    }
}
