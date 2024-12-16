package com.iot;

import com.iot.modbus.ModbusTcpClient;
import com.iot.mqtt.MqttSub;
import com.iot.mqtt.MqttToDB;

public class mainPoint {
    public static void main(String[] args) {
        Thread modbusThread = new Thread(new ModbusTcpClient());
        // Thread mqttSubThread = new Thread(new MqttSub());
        Thread mqttToDB = new Thread(new MqttToDB());

        modbusThread.start();
        // mqttSubThread.start();
        mqttToDB.start();
    }
}