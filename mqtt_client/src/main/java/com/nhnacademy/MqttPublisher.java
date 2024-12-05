package com.nhnacademy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.paho.client.mqttv3.*;

// mqtt out (발신)

public class MqttPublisher {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String CLIENT_ID = "MyJavaClientPublisher";
    private static final String MOSQUITTO_BROKER = "tcp://192.168.71.204:1883"; 

    // MQTT 메시지 발행 메서드
    public void publishMessage(String broker, String topic, String message) {
        try (MqttClient mqttClient = new MqttClient(MOSQUITTO_BROKER, CLIENT_ID)) {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            mqttClient.connect(options);

            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(1); // QoS 설정

            // Mosquitto 브로커로 발행
            mqttClient.publish(topic, mqttMessage);
            log.debug("발행: {} -> {}", topic, message);
            mqttClient.disconnect();
        } catch (MqttException e) {
            log.error("메시지 발행 중 오류 발생: {}", e.getMessage());
            e.printStackTrace();
        }
    }
}
