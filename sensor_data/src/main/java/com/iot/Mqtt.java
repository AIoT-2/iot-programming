package com.iot;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

public class Mqtt {
    // MQTT 브로커 주소
    private static final String BROKER = "tcp://192.168.70.203:1883";
    // 클라이언트 ID
    private static final String CLIENT_ID = "JavaClientExample";
    // 구독 및 발행 주제
    private static final String TOPIC = "data/#";

    public static void main(String[] args) throws InterruptedException {
        try (MqttClient client = new MqttClient(BROKER, CLIENT_ID)) {

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
                public void messageArrived(String topic, MqttMessage message) throws Exception {

                    String payload = new String(message.getPayload());

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("topic", topic);
                    jsonObject.put("data", new JSONObject(payload));

                    System.out.println(jsonObject.toString(4));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("Message delivery complete: "
                            + token.getMessageId());
                }
            });

            // 브로커 연결
            System.out.println("Connecting to broker...");
            client.connect(options);
            System.out.println("Connected!");

            // 주제 구독
            System.out.println("Subscribing to topic: " + TOPIC);
            client.subscribe(TOPIC);

            // 10초 대기 후 종료
            Thread.sleep(100000);

            // 클라이언트 종료
            System.out.println("Disconnecting...");
            client.disconnect();
            System.out.println("Disconnected!");

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
