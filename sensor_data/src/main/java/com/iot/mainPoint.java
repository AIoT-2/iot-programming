package com.iot;

import com.iot.modbus.ModbusTcpClient;
import com.iot.mqtt.Mqtt;

public class mainPoint {
    public static void main(String[] args) {
        Thread modbusThread = new Thread(new ModbusTcpClient());
        Thread mqttThread = new Thread(new Mqtt());

        modbusThread.start();
        mqttThread.start();
    }
}