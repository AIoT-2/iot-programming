package com.iot.modbus;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersRequest;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersResponse;

public class newDataProcessing {
    static final Logger logger = LoggerFactory.getLogger(newDataProcessing.class);
    static String clientId = "songs";

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

    public static void processModbusData(List<Map<String, Object>> locationList, List<Map<String, Object>> channelList,
            ModbusMaster master, MqttClient mqttClient) throws ModbusTransportException {
        ObjectMapper mapper = new ObjectMapper();

        for (Map<String, Object> location : locationList) {
            int channel = (int) location.get("Channel");
            String locationName = (String) location.get("위치");
            int baseAddress = channel * 100;

            for (Map<String, Object> channelData : channelList) {
                int offset = (int) channelData.get("offset");
                String type = (String) channelData.get("type");
                String name = (String) channelData.get("name");
                int size = (int) channelData.get("size");
                int scale = channelData.get("scale") instanceof Integer ? (int) channelData.get("scale") : 1;

                int registerAddress = baseAddress + offset;

                ReadHoldingRegistersResponse response = readRegisters(master, registerAddress, size);
                if (response == null || response.isException())
                    continue;

                publishModbusData(mqttClient, mapper, response.getShortData(), scale, type, name, locationName);
            }
        }
    }

    private static void publishModbusData(MqttClient mqttClient, ObjectMapper mapper, short[] values, int scale,
            String type,
            String name, String locationName) {
        try {
            for (short value : values) {
                double scaledValue = Math.abs((double) value / scale);
                if (scaledValue == 0)
                    continue;

                String topic = String.format("%s/n/%s/p/%s/e/%s", clientId, name, locationName, scaledValue);
                Map<String, Object> payload = Map.of(
                        "time", Instant.now().toEpochMilli(),
                        "value", scaledValue);

                MqttMessage message = new MqttMessage(mapper.writeValueAsString(payload).getBytes());
                mqttClient.publish(topic, message);
                logger.info("Published message: {} to topic: {}", payload, topic);
            }
        } catch (Exception e) {
            logger.error("Failed to publish MQTT message: {}", e.getMessage());
        }
    }

    private static ReadHoldingRegistersResponse readRegisters(ModbusMaster master, int address, int size) {
        try {
            ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(1, address, size);
            return (ReadHoldingRegistersResponse) master.send(request);
        } catch (ModbusTransportException e) {
            logger.error("Modbus communication error: {}", e.getMessage());
            return null;
        }
    }
}
