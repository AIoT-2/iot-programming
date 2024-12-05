package com.nhnacademy.mqtt;

import com.nhnacademy.mqtt.impl.MqttClientImpl;

public class MqttMain {

    public static void main(String[] args) {
        Thread thread = new Thread(new MqttClientImpl());
        thread.start();
    }
}
