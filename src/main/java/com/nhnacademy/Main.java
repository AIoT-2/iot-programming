package com.nhnacademy;

import com.nhnacademy.db.InfluxDB;
import com.nhnacademy.modbus.ModbusToMqtt;
import com.nhnacademy.mqtt.MqttSubscriber;

public class Main {
    public static void main(String[] args) {
        Thread t1 = new Thread(new InfluxDB());
        Thread t2 = new Thread(new ModbusToMqtt());
        Thread t3 = new Thread(new MqttSubscriber());

        t1.start();
        t2.start();
        t3.start();
    }
}