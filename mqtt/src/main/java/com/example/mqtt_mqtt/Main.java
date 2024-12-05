package com.example.mqtt_mqtt;



public class Main {
    public static void main(String[] args) {
        MqttPublisher publisher = new MqttPublisher();
        MqttSubscriber subscriber = new MqttSubscriber(publisher);

        subscriber.start();

        // Publisher 종료
        Runtime.getRuntime().addShutdownHook(new Thread(publisher::disconnect));
    }
}