package com.totalmqtt;

import java.util.Map;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SendBroker {
    // MQTT 브로커 주소
    private static final String BROKER = "tcp://192.168.71.205:1883";
    // 클라이언트 ID
    private static final String CLIENT_ID = "";

    private MqttClient client;

    public SendBroker() {
        try {
            client = new MqttClient(BROKER, CLIENT_ID);
            // 연결 설정
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true); // 클린 세션 사용

            // 메시지 수신 콜백 설정
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Connection lost: "
                            + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message)
                        // subscribe가 데이터를 받을시
                        throws Exception {
                    System.out.println("Received message from topic '"
                            + topic + "': " + new String(message.getPayload()));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // publish가 데이터를 보낼시
                    System.out.println("Message delivery complete: "
                            + token.getMessageId());
                }
            });

            // 브로커 연결
            System.out.println("Connecting to broker...");
            client.connect(options);
            System.out.println("Connected!");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void send(Map<String, Object> data, String topic) throws InterruptedException {
        try{

            // ObjectMapper 객체 생성
            ObjectMapper objectMapper = new ObjectMapper();
            // publish
            String message = objectMapper.writeValueAsString(data);
            System.out.println("Publishing message: " + message);
            client.publish(topic, new MqttMessage(message.getBytes()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
