package com.example;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MQTTSubscriber {
    // 브로커 설정
    private static final String SOURCE_BROKER = "tcp://192.168.70.203:1883";
    private static final String DEST_BROKER = "tcp://localhost:1883";
    private static final String SOURCE_CLIENT_ID = "SourceSubscriber";
    private static final String DEST_CLIENT_ID = "DestPublisher";
    private static final String SOURCE_TOPIC = "application/#";
    private static final String DEST_TOPIC_PREFIX = "sensor";

    public static void main(String[] args) {
        try {
            // 소스 브로커에서 구독할 클라이언트
            MqttClient sourceClient = new MqttClient(SOURCE_BROKER, SOURCE_CLIENT_ID);
            // 대상 브로커로 발행할 클라이언트
            MqttClient destClient = new MqttClient(DEST_BROKER, DEST_CLIENT_ID);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);

            // 양쪽 브로커에 연결
            sourceClient.connect(options);
            destClient.connect(options);

            sourceClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Source broker connection lost: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload = new String(message.getPayload());
                    System.out.println("Received from source - Topic: " + topic);
                    System.out.println("Payload: " + payload);

                    // 대상 브로커로 전송
                    MqttMessage newMessage = new MqttMessage(payload.getBytes());
                    newMessage.setQos(1);
                    destClient.publish(DEST_TOPIC_PREFIX + "/" + topic, newMessage);
                    System.out.println("Published to destination broker");
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("Message delivered");
                }
            });

            sourceClient.subscribe(SOURCE_TOPIC);
            System.out.println("Subscribed to: " + SOURCE_TOPIC);

            // 프로그램 종료 처리
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    sourceClient.disconnect();
                    destClient.disconnect();
                    System.out.println("Disconnected from brokers");
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }));

            // 계속 실행
            while (true) {
                Thread.sleep(1000);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}