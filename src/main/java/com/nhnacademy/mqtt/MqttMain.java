package com.nhnacademy.mqtt;

public class MqttMain {

    public static void main(String[] args) {
        Thread thread = new Thread(new MqttClientImpl());
        thread.start();
    }
}
