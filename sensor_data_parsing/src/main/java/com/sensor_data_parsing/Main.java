package com.sensor_data_parsing;

import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.sensor_data_parsing.threads.ModbusToMqtt;
import com.sensor_data_parsing.threads.MqttToInfluxDB;
import com.sensor_data_parsing.threads.MqttToMqtt;

public class Main {
    public static void main(String[] args) {
        MqttClient sub = new MqttClient("sub", "192.168.70.203", 1883);
        MqttClient subAndPub = new MqttClient("subAndPub", "localhost", 8888);
        TcpConnect tcpParameters = new TcpConnect();
        MqttToMqtt mqttToMqtt = new MqttToMqtt(sub, subAndPub);
        ModbusToMqtt modbusToMqtt = new ModbusToMqtt(subAndPub, tcpParameters.getTcpParameters(), 1, 100, 32, 1);

        // Mqtt로 받은 데이터를 InfluxDB에 저장
        new Thread(new MqttToInfluxDB(subAndPub)).start();

        // Modbus로 받은 데이터를 Mqtt브로커에 전달
        new Thread(() -> {
            try {
                if (!modbusToMqtt.getModbusMaster().isConnected()) {
                    modbusToMqtt.getModbusMaster().connect();
                }

                while (!Thread.currentThread().isInterrupted()) {
                    String data = modbusToMqtt.fetchDataFromProtocol();
                    String[] convertData = modbusToMqtt.convertToMqttFormat(data);
                    modbusToMqtt.sendMessageToMqtt(convertData);

                    // 30초 대기
                    modbusToMqtt.waitForNextCycle(30);
                }
            } catch (ModbusIOException e) {
                System.err.println("Modbus 연결 오류: " + e.getMessage());
            } finally {
                // 스레드 종료 시 연결 해제
                try {
                    if (modbusToMqtt.getModbusMaster().isConnected()) {
                        modbusToMqtt.getModbusMaster().disconnect();
                    }
                } catch (ModbusIOException e) {
                    System.err.println("Modbus 연결 해제 오류: " + e.getMessage());
                }
            }
        }).start();

        // Mqtt로 받은 데이터를 Mqtt브로커에 전달
        new Thread(() -> {
            String data = mqttToMqtt.fetchDataFromProtocol();
            String[] convertData = mqttToMqtt.convertToMqttFormat(data);
            mqttToMqtt.sendMessageToMqtt(convertData);
        }).start();
    }
}
