package com.nhnacademy;

public class Main {
    public static void main(String[] args){
        MqttReceiver receiver = new MqttReceiver();
        receiver.startReceiving();
    }
}
