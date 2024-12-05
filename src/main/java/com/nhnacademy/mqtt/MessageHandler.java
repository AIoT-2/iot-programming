package com.nhnacademy.mqtt;

public interface MessageHandler {
    void processMessage(String topic, String message);
}
