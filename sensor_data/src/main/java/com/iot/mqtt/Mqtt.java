package com.iot.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Mqtt extends MqttTransform implements Runnable {
    // MQTT 브로커 주소
    private static final String BROKER = "tcp://192.168.70.203:1883";
    // 클라이언트 ID
    private static final String CLIENT_ID = "song";
    // 구독 및 발행 주제
    private static final String TOPIC = "data/#";

    public void run() {

        try (MqttClient client = new MqttClient(BROKER, CLIENT_ID)) {

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            // 메시지 수신 콜백 설정
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Connection lost: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {

                    String payload = new String(message.getPayload());
                    ObjectMapper objectMapper = new ObjectMapper();
                    String measurement = extractName(topic);

                    try {
                        JsonNode jsonNode = objectMapper.readTree(payload);

                        if (topic.contains("lora") || topic.contains("power_meter")) {
                            return;
                        }

                        System.out.println("Measurement : " + extractElement(topic));
                        System.out.println("Value : " + jsonNode.get("value").asDouble());
                        System.out.println("Place: " + extractPlace(topic));
                        System.out.println("Spot: " + measurement);
                        System.out.println("Topic: " + topic);
                        System.out.println("msg: " + jsonNode.toString());
                        System.out.println();
                    } catch (Exception e) {
                        System.out.println("Invalid JSON payload: " + payload);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("Message delivery complete: "
                            + token.getMessageId());
                }
            });

            System.out.println("Connecting to broker...");
            client.connect(options);
            System.out.println("Connected!");

            System.out.println("Subscribing to topic: " + TOPIC);
            client.subscribe(TOPIC);

            try {
                Thread.sleep(100000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("Disconnecting...");
            client.disconnect();
            System.out.println("Disconnected!");

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}