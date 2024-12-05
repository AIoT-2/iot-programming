package com.nhnacademy.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.nhnacademy.mqttpub.MqttPub;

import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
public class MqttBrokers {
    private static MqttClient client = null; // MqttClient를 클래스 변수로 관리

    private MqttBrokers() {
        // ignore
    }

    public static void startListening(String brokerUrl, String topic, MqttPub mqttPub) {
        try {
            if (client == null) {
                client = new MqttClient(brokerUrl, MqttClient.generateClientId());
                MqttConnectOptions options = new MqttConnectOptions();
                options.setCleanSession(true);
                client.connect(options);
                log.debug("Connected to MQTT Broker: " + brokerUrl);
            }

            // MQTT 메시지 수신 콜백 설정
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.error("Connection lost: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload = new String(message.getPayload());
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode jsonMessage = objectMapper.readTree(payload);

                    JsonNode object = jsonMessage.get("object");
                    JsonNode deviceInfo = jsonMessage.get("deviceInfo");

                    if (object != null && deviceInfo != null) {
                        // MqttPub을 사용하여 InfluxDB에 기록
                        mqttPub.writeToInfluxDB(object, deviceInfo);
                    } else {
                        log.debug("Missing data in message.");
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    log.error("Message delivery complete: " + token.getMessageId());
                }
            });

            client.subscribe(topic);
            log.debug("Subscribed to topic: " + topic);

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // 클라이언트를 종료하는 메서드
    public static void stopListening() {
        if (client != null && client.isConnected()) {
            try {
                client.disconnect();
                log.info("Disconnected from MQTT Broker.");
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            log.info("Client is not connected.");
        }
    }
}
