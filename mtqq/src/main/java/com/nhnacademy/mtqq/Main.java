package com.nhnacademy.mtqq;

public class Main {
    public static void main(String[] args){
        MqttToInfluxDB mqttToInfluxDB = new MqttToInfluxDB();
        try {
            mqttToInfluxDB.run();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
