package com.nhnacademy.mtqq.exception;

public class MqttMessageNotFoundException extends RuntimeException{
    public MqttMessageNotFoundException(String message){
        super(message);
    }
}
