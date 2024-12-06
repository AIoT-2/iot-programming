package com.nhnacademy.mqtt;

import java.util.Objects;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MqttPublisher {
 
    private MqttClient client;

    public MqttPublisher(String serverURI, String clientId){
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
    
    public void send(String topic, String message) {
        if(Objects.isNull(topic)){
            throw new IllegalArgumentException("topic is null");
        }
        if(Objects.isNull(message)){
            throw new IllegalArgumentException("message is null");
        }

        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(1);
            client.publish(topic, mqttMessage);

            log.debug("Message published to topic: {}", topic);
            log.debug("Message: {}", mqttMessage);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void send(String topic, String message, int qos) {
        if(qos < 0 || qos > 2){
            throw new IllegalArgumentException("you can input qos only [0, 1, 2]");
        }
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(qos);
            client.publish(topic, mqttMessage);

            log.debug("Message published to topic: {}", topic);
            log.debug("Message: {}", mqttMessage);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void close(){
        if(client.isConnected()){
            try {
                log.debug("Publisher: Disconnecting...");
                client.disconnect();
                client.close();
                log.debug("Publisher: Disconnected!");
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

}