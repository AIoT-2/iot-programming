package com.iot.modbus;

import java.io.File;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class DataProcessing {
    static final Logger logger = LoggerFactory.getLogger(DataProcessing.class);

    static int slaveId = 1;
    static String clientId = "songs";

    public static void processing(List<Map<String, Object>> locationList, List<Map<String, Object>> channelList,
            ModbusMaster master) {

        for (Map<String, Object> location : locationList) {
            int channel = (int) location.get("Channel");
            String locationName = (String) location.get("위치");

            int baseAddress = channel * 100;

            for (Map<String, Object> channelData : channelList) {
                int offset = (int) channelData.get("offset");
                String type = (String) channelData.get("type");
                String name = (String) channelData.get("name");
                int size = (int) channelData.get("size");
                Object scaleObj = channelData.get("scale");
                int scale = (scaleObj != null) ? (int) scaleObj : 1;

                int registerAddress = baseAddress + offset;

                if (type == "UNIT32") {
                    combineregister(registerAddress, scale);
                }
                if (type == "INT32") {
                    combineRegistersToInt(registerAddress, scale);
                }

                ReadHoldingRegistersRequest request;

                ReadHoldingRegistersResponse response;

                try {
                    request = new ReadHoldingRegistersRequest(slaveId,
                            registerAddress,
                            size);

                    response = (ReadHoldingRegistersResponse) master.send(request);
                    if (response.isException()) {
                        System.out.println("Modbus Error (Address " + registerAddress + "): "
                                + response.getExceptionMessage());
                    } else {
                        short[] values = response.getShortData();
                        for (int i = 0; i < values.length; i++) {
                            double scaledValue = (double) values[i] / scale;

                            if (scaledValue == 0) {
                                continue;
                            }
                            if (scaledValue < 0) {
                                scaledValue *= -1;
                            }

                            if (type.contains("32")) {
                                registerAddress += 1;
                            }

                            String topic = clientId + "/n/" + name + "/p/" + locationName + "/e/" + scaledValue;
                            // Create Message with time and value
                            ObjectMapper objectMapper = new ObjectMapper();
                            Map<String, Object> messagePayload = new HashMap<>();
                            messagePayload.put("time", Instant.now().toEpochMilli());
                            messagePayload.put("value", scaledValue);
                        }
                    }
                } catch (ModbusTransportException e) {
                    System.err.println(e);
                }

            }
        }
    }

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

    // reg1 = registeraddress, reg2 = scale
    public static long combineregister(int reg1, int reg2) {
        return ((long) (reg1 & 0xFFFF) << 16) | (reg2 & 0xFFFF);
    }

    public static int combineRegistersToInt(int reg1, int reg2) {
        return (reg1 << 16) | (reg2 & 0xFFFF);
    }
}
