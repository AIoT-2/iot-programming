package com.nhnacademy.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttPublisher {

    private final Logger logger = LoggerFactory.getLogger(getClass());
 
    private MqttClient client;

    public MqttPublisher(String serverURI, String clientId){
        try {
            this.client = new MqttClient(serverURI, clientId);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            client.connect(options);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    public void send(String topic, String message, int qos) {
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(qos);
            client.publish(topic, mqttMessage);

            logger.info("Message published to topic: {}", topic);
            logger.info("Message: {}", mqttMessage);
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