package com.sensor_data_parsing;

import com.sensor_data_parsing.threads.ModbusToMqtt;
import com.sensor_data_parsing.threads.MqttToInfluxDB;
import com.sensor_data_parsing.threads.MqttToMqtt;

// 로그 추가 필요
public class Main {
    public static void main(String[] args) {
        MqttClient sub = new MqttClient("sub", "192.168.70.203", 1883);
        MqttClient subAndPub = new MqttClient("subAndPub", "localhost", 8888);
        TcpConnect tcpParameters = new TcpConnect();

        // Mqtt로 받은 데이터를 InfluxDB에 저장
        new Thread(new MqttToInfluxDB(subAndPub)).start();

        // Modbus로 받은 데이터를 Mqtt브로커에 전달
        new Thread(new ModbusToMqtt(subAndPub, tcpParameters.getTcpParameters(), 1, 100, 32, 1)).start();

        // Mqtt로 받은 데이터를 Mqtt브로커에 전달
        new Thread(new MqttToMqtt(sub, subAndPub)).start();
    }
}
