package com.sensor_data_flow;

import com.sensor_data_flow.client.ModbusTcpClient;
import com.sensor_data_flow.client.MqttClient;
import com.sensor_data_flow.threads.ModbusToMqtt;
import com.sensor_data_flow.threads.MqttToMqtt;

public class Main {
    public static void main(String[] args) {
        MqttClient sub = new MqttClient("sub", "192.168.70.203", 1883);
        MqttClient subAndPub = new MqttClient("subAndPub", "localhost", 8888);
        TcpConnect tcpConnect = new TcpConnect();
        ModbusTcpClient client = new ModbusTcpClient(tcpConnect.getTcpParameters());
        client.setModbusRequest(1, 100, 32, 1);

        // Mqtt로 받은 데이터를 InfluxDB에 저장
        // new Thread(new MqttToInfluxDB(subAndPub)).start();

        // Modbus로 받은 데이터를 Mqtt브로커에 전달
        new Thread(new ModbusToMqtt(subAndPub, client)).start();

        // Mqtt로 받은 데이터를 Mqtt브로커에 전달
        new Thread(new MqttToMqtt(sub, subAndPub)).start();
    }
}
