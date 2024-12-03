package com.nhnacademy.mqttPub;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttPub {
    private MqttClient mqttClient;

    // MQTT 브로커에 연결하는 메서드
    public void connectToBroker(String brokerUrl) throws MqttException {
        mqttClient = new MqttClient(brokerUrl, MqttClient.generateClientId());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        mqttClient.connect(options);
        System.out.println("Connected to " + brokerUrl);
    }

    // MQTT 메시지 발행
    public void publishMessage(String topic, String message) {
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttClient.publish(topic, mqttMessage);
            System.out.println("Published message to topic: " + topic + ", Message: " + message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // MQTT 연결 해제
    public void disconnect() {
        try {
            if (mqttClient.isConnected()) {
                mqttClient.disconnect();
                System.out.println("Disconnected from broker");
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
