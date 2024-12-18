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

        new Thread(() -> {
            try {
                Thread.sleep(20); // 20초 대기
                System.out.println("20초 지났습니다. 하지만 스레드는 계속 실행됩니다.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}