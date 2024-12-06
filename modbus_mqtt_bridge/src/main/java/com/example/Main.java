package com.example;

public class Main {
    public static void main(String[] args){
        MqttReceiver receiver = new MqttReceiver();
        receiver.startReceiving();
    }
}
