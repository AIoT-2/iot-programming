package com.nhnacademy.mqtt;

import java.util.Objects;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MqttSubscriber{
    private MqttClient client;
    private MessageHandler messageHandler;

    public MqttSubscriber(String serverURI, String clientId){
        if(Objects.isNull(clientId) || Objects.isNull(serverURI)){
            throw new IllegalArgumentException("serverURI 또는 cliendId가 없습니다.");
        }

        try {
            this.client = new MqttClient(serverURI, clientId);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            client.connect(options);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public MqttSubscriber(String serverURI, String clientId, MessageHandler messageHandler) {
        try {
            this.messageHandler = messageHandler;
            this.client = new MqttClient(serverURI, clientId);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            client.connect(options);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void getMessage(String topic){
        try {
            log.debug("Subscribing to topic: {}", topic);
            client.setCallback(new MqttCallbackImpl(client, messageHandler));
            client.subscribe(topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    

    public void close(){
        if(client.isConnected()){
            try {
                log.debug("Subscriber: Disconnecting...");
                client.disconnect();
                client.close();
                log.debug("Subscriber: Disconnected!");
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

}
