package com.iot.modbus;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersRequest;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersResponse;
import com.fasterxml.jackson.core.type.TypeReference;

public class ModbusTcpClient {
    public static void main(String[] args) throws IOException {

        String setHost = "192.168.70.203";
        int setPort = 502;
        int slaveId = 1;

        String locationFilePath = "/home/nhnacademy/문서/iot/sensor_data/src/main/java/com/iot/modbus/location.json";
        String channelFilePath = "/home/nhnacademy/문서/iot/sensor_data/src/main/java/com/iot/modbus/channel.json";
        String addmapFilePath = "/home/nhnacademy/문서/iot/sensor_data/src/main/java/com/iot/modbus/addmap.json";

        // MQTT Configuration
        String broker = "tcp://192.168.70.203:1883"; // MQTT 브로커 주소
        String clientId = "ModbusMqttClient_song";
        String topic = "songsong"; // 전송할 MQTT 주제
        int qos = 2; // QoS level

        try (MqttClient mqttClient = new MqttClient(broker, clientId, new MemoryPersistence())) {
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            mqttClient.connect(connOpts);
            System.out.println("Connected to MQTT broker: " + broker);

            List<Map<String, Object>> locationList = loadJsonData(locationFilePath);
            List<Map<String, Object>> channelList = loadJsonData(channelFilePath);
            List<Map<String, Object>> addmapList = loadJsonData(addmapFilePath);

            IpParameters ipParameters = new IpParameters();
            ipParameters.setHost(setHost);
            ipParameters.setPort(setPort);

            ModbusFactory modbusFactory = new ModbusFactory();
            ModbusMaster master = modbusFactory.createTcpMaster(ipParameters, true);

            try {
                master.init();
                System.out.println("Connected to Modbus server");

                for (Map<String, Object> location : locationList) {
                    int channel = (int) location.get("Channel");
                    String locationName = (String) location.get("위치");

                    int baseAddress = channel * 100;

                    for (Map<String, Object> channelData : channelList) {
                        int offset = (int) channelData.get("offset");
                        String name = (String) channelData.get("name");
                        String type = (String) channelData.get("type");
                        int size = (int) channelData.get("size");
                        Object scaleObj = channelData.get("scale");
                        int scale = (scaleObj != null) ? (int) scaleObj : 1;

                        int registerAddress = baseAddress + offset;

                        ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(slaveId, registerAddress,
                                size);
                        ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) master.send(request);

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
                                System.out.print(locationName);

                                if (type.contains("32")) {
                                    registerAddress += 1;
                                }
                                System.out.print("/ " + registerAddress);
                                System.out.print("/ " + name);
                                System.out.println("/ " + scaledValue);
                                System.out.println("-----------------------------------");
                            }
                        }
                    }
                }

                for (Map<String, Object> commonData : addmapList) {
                    int offset = (int) commonData.get("address");
                    String name = (String) commonData.get("name");
                    String type = (String) commonData.get("type");
                    int size = (int) commonData.get("size");
                    Object scaleObj = commonData.get("scale");
                    int scale = (scaleObj != null) ? (int) scaleObj : 1;

                    int registerAddress = offset;

                    ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(slaveId, registerAddress,
                            size);
                    ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) master.send(request);

                    if (response.isException()) {
                        System.out.println(
                                "Modbus Error (Address " + registerAddress + "): " + response.getExceptionMessage());
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
                            System.out.print(name);
                            System.out.print("/ " + registerAddress);
                            System.out.println("/ " + scaledValue);
                            System.out.println("-----------------------------------");
                        }
                    }
                }
            } catch (ModbusInitException e) {
                System.err.println("Failed to initialize Modbus Master: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Error in Modbus communication: " + e.getMessage());
            } finally {
                master.destroy();
                System.out.println("Disconnected from Modbus server.");
            }

        } catch (MqttException e) {
            System.err.println("Error in MQTT communication: " + e.getMessage());
        }
    }

    private static List<Map<String, Object>> loadJsonData(String jsonFilePath) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(new File(jsonFilePath), new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (IOException e) {
            System.out.println("Failed to load JSON file: " + e.getMessage());
            return null;
        }
    }
}
