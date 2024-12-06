package com.nhnacademy.modbus;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelligt.modbus.jlibmodbus.msg.response.ReadInputRegistersResponse;
import com.nhnacademy.mqtt.MqttPublisher;

public class Main {

    private static final String MQTT_BROKER_URL = "tcp://192.168.71.222:1883";
    private static final String MQTT_CLIENT_ID = "JavaClientExample";

    private static Map<Integer, String> addressMap = new HashMap<>();
    private static Map<Integer, Map<String, Object>> offsetMap = new HashMap<>();
    private static ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        ModbusConfig modbus = new ModbusConfig("192.168.70.203");
        MqttPublisher mqttPublish = new MqttPublisher(MQTT_BROKER_URL, MQTT_CLIENT_ID);
        try {
            addressMap = ReadChannelData.loadChannelData("src/main/resources/channel.json");
            offsetMap = ReadChannelData.loadOffsetData("src/main/resources/channelInfo.json");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            for (Map.Entry<Integer, String> addressEntry : addressMap.entrySet()) {
                try {
                    int slaveId = 1; // Channel??, unitId
                    int channel = addressEntry.getKey(); // address
                    ReadInputRegistersResponse response = modbus.readInputRegistersResponse(slaveId, channel, 32);
                    if (response != null) {
                        Map<String, Object> dataMap = getDataMap(response, channel);
                        Map<String, Object> dataMessage = new HashMap<>();
                        for(Map.Entry<String, Object> entry : dataMap.entrySet()){
                            String key = entry.getKey();
                            Object value = entry.getValue();
                            dataMessage.put("time", new Timestamp(System.currentTimeMillis()));
                            dataMessage.put("value", value);
                            String message = mapper.writeValueAsString(dataMessage);
                            mqttPublish.send("modbus/" + addressEntry.getValue() + "/e/" + key, message);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } finally {
            modbus.close();
            mqttPublish.close();
        }
    }

    public static Map<String, Object> getDataMap(ReadInputRegistersResponse response, int startAddress) {
        Map<String, Object> dataMap = new HashMap<>();
        for (int offset = 0; offset < response.getHoldingRegisters().getQuantity(); offset++) {
            int registerValue;
            try {
                registerValue = response.getHoldingRegisters().get(offset);

                String addressName = (String) offsetMap.get(offset).get("name");
                int scale = (int) offsetMap.get(offset).get("scale");

                if ((int) offsetMap.get(offset).get("size") == 2) {
                    int firstRegisterValue = response.getHoldingRegisters().get(offset);
                    int secondRegisterValue = response.getHoldingRegisters().get(offset + 1);
                    registerValue = (firstRegisterValue << 16) | secondRegisterValue;
                    offset++;
                }

                double realRegisterValue = registerValue / scale;
                dataMap.put(addressName, checkDouble(realRegisterValue) ? (int) realRegisterValue : (double) realRegisterValue);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return dataMap;
    }

    public static boolean checkDouble(double realRegisterValue) {
        return realRegisterValue == Math.floor(realRegisterValue);
    }
}
