package com.iot.modbus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.FileSystems;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersRequest;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ModbusTcpClient extends ModbusTransform implements Runnable {
    static final Logger logger = LoggerFactory.getLogger(ModbusTcpClient.class);

    String setHost = "192.168.70.203";
    int setPort = 502;
    int slaveId = 1;

    String locationFilePath = FileSystems.getDefault().getPath("resources/location.json")
            .toAbsolutePath().toString();
    String channelFilePath = FileSystems.getDefault().getPath("resources/channel.json")
            .toAbsolutePath().toString();

    // MQTT Configuration
    String broker = "tcp://192.168.70.203:1883";
    String clientId = "songs";

    public void run() {
        MqttClient mqttClient = null;

        while (true) { // 무한 재시도 루프
            try {
                // MQTT 연결
                if (mqttClient == null || !mqttClient.isConnected()) {
                    mqttClient = connectToMQTT();
                }

                // Modbus Master 설정
                IpParameters ipParameters = new IpParameters();
                ipParameters.setHost(setHost);
                ipParameters.setPort(setPort);

                ModbusFactory modbusFactory = new ModbusFactory();
                ModbusMaster master = modbusFactory.createTcpMaster(ipParameters, true);

                try {
                    master.init();
                    System.out.println("Connected to Modbus server");

                    List<Map<String, Object>> locationList = loadJsonData(locationFilePath);
                    List<Map<String, Object>> channelList = loadJsonData(channelFilePath);

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

                            ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(slaveId,
                                    registerAddress, size);
                            ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) master.send(request);

                            if (response.isException()) {
                                System.out.println("Modbus Error (Address " + registerAddress + "): "
                                        + response.getExceptionMessage());
                            } else {
                                short[] values = response.getShortData();
                                for (int i = 0; i < values.length; i++) {
                                    double scaledValue = (double) values[i] / scale;

                                    if (scaledValue == 0)
                                        continue;
                                    if (scaledValue < 0) {
                                        scaledValue *= -1;
                                    }

                                    if (type.contains("32")) {
                                        registerAddress += 1;
                                    }

                                    convertAndScale(values, type, scaledValue);

                                    String topic = clientId + "/place/" + locationName + "/valueName/" + name
                                            + "/value/" + scaledValue;
                                    Map<String, Object> messagePayload = new HashMap<>();
                                    messagePayload.put("payload", name);
                                    messagePayload.put("time", Instant.now().toEpochMilli());
                                    messagePayload.put("value", scaledValue);

                                    String message = new ObjectMapper().writeValueAsString(messagePayload);

                                    MqttMessage mqttMessage = new MqttMessage(message.getBytes());
                                    mqttMessage.setQos(1);
                                    mqttClient.publish(topic, mqttMessage);

                                    // logger.debug("Published: " + message);
                                    // logger.debug("topic: " + topic);

                                }
                            }
                        }
                        Thread.sleep(5000);
                    }
                } catch (ModbusInitException | ModbusTransportException e) {
                    System.err.println("Modbus Error: " + e.getMessage());
                    TimeUnit.SECONDS.sleep(10); // 10초 대기 후 재시도
                } finally {
                    master.destroy();
                }
            } catch (Exception e) {
                System.err.println("General Error: " + e.getMessage());
                try {
                    TimeUnit.SECONDS.sleep(10); // 10초 대기 후 재시도
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private static double convertAndScale(short[] values, String type, double scale) {
        long rawValue = 0;
        for (short value : values) {
            rawValue = (rawValue << 16) | (value & 0xFFFF);
        }
        switch (type.toUpperCase()) {
            case "UINT16":
                return (values.length == 1 ? (values[0] & 0xFFFF) : rawValue) / scale;
            case "UINT32":
                return rawValue / scale;
            case "INT16":
                return values.length == 1 ? values[0] / scale : rawValue / scale;
            case "INT32":
                return (rawValue > 0x7FFFFFFF ? rawValue - 0x100000000L : rawValue) / scale;
            default:
                System.err.println("알 수 없는 데이터 타입: " + type);
                return 0;
        }
    }

    private MqttClient connectToMQTT() throws MqttException {
        MqttClient mqttClient = new MqttClient(broker, clientId);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        mqttClient.connect(connOpts);
        System.out.println("Reconnected to MQTT broker: " + broker);
        return mqttClient;
    }

    public static void main(String[] args) {
        Thread modbusThread = new Thread(new ModbusTcpClient());
        modbusThread.start();
    }
}