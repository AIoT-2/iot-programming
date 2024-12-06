package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.paho.client.mqttv3.*;

// mqtt out (발신)
public class MqttPublisher {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private String publisherBroker;
    private String clientId;
    private String topic;

    public MqttPublisher() {
        // ConfigReader를 사용하여 프로퍼티 파일에서 설정값을 읽어옵니다.
        ConfigReader configReader = new ConfigReader("application.properties");
        publisherBroker = configReader.getProperty("mqtt.publisher.broker");
        clientId = configReader.getProperty("mqtt.publis.clientId");
        topic = configReader.getProperty("mqtt.receiver.topic");
    }


    // MQTT 메시지 발행 메서드
    public void publishMessage(String broker, String topic, String message) {
        try (MqttClient mqttClient = new MqttClient(publisherBroker, clientId)) {
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
