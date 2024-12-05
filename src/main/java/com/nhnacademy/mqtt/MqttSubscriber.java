package com.nhnacademy.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;


public class MqttSubscriber{
    private MqttClient client;
    private MessageHandler messageHandler;


    public MqttSubscriber(String serverURI, String clientId){
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
            client.setCallback(new MqttCallbackImpl(client, messageHandler));
            client.subscribe(topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    

    public void close(){
        if(client.isConnected()){
            try {
                client.disconnect();
                client.close();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

}
