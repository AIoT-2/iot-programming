package com.iot;

import com.iot.modbus.ModbusTcpClient;
import com.iot.mqtt.MqttPub;
import com.iot.mqtt.MqttSub;

public class mainPoint {
    public static void main(String[] args) {
        Thread modbusThread = new Thread(new ModbusTcpClient());
        Thread mqttPubThread = new Thread(new MqttPub());
        Thread mqttSubThread = new Thread(new MqttSub());

        modbusThread.start();
        mqttPubThread.start();
        mqttSubThread.start();
    }
}