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
    private MqttBrokers() {
        // ignore
    }

    public static void startListening(String brokerUrl, String topic, MqttPub mqttPub) {
        try {
            // MainApp에서 닫아서 여기서는 client를 닫는 것을 만들지 않았다.
            MqttClient client = new MqttClient(brokerUrl, MqttClient.generateClientId());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            client.connect(options);
            System.out.println("Connected to MQTT Broker: " + brokerUrl);

            // MQTT 메시지 수신 콜백 설정
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Connection lost: " + cause.getMessage());
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
                        System.out.println("Missing data in message.");
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("Message delivery complete: " + token.getMessageId());
                }
            });

            client.subscribe(topic);
            System.out.println("Subscribed to topic: " + topic);

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
