package com.nhnacademy.mqtt;

public class test {
    public static void main(String[] args) {
        Thread t3 = new Thread(new MqttSubscriber("192.168.70.203", "", "", "application/#"));
        t3.start();
    }
}
