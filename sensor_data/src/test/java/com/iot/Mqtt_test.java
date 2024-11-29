package com.iot;

import org.eclipse.paho.client.mqttv3.*;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class Mqtt_test {
    private static final String BROKER = "tcp://192.168.70.203:1883";
    private static final String CLIENT_ID = "JavaClientTest";
    private static final String TOPIC = "data/test";
    private MqttClient client;

    @Before
    public void setUp() throws Exception {
        // MQTT 클라이언트 생성 및 브로커 연결
        client = new MqttClient(BROKER, CLIENT_ID);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        client.connect(options);
    }

    @After
    public void tearDown() throws Exception {
        if (client != null && client.isConnected()) {
            client.disconnect();
        }
    }

    @Test
    public void testMessageArrived() throws Exception {
        // 테스트용 메시지 발행
        String testMessage = "{\"time\":1732861376312,\"type\":2,\"leakageCurrent\":0,\"current\":14,\"activePower\":4,\"reactivePower\":-28,\"tapparentPower\":33,\"powerFactor\":14.28,\"currentUnbalance\":0,\"lines\":[]}";

        // MQTT 메세지 객체 생성 및 QoS 설정
        MqttMessage message = new MqttMessage(testMessage.getBytes());
        message.setQos(1);

        client.publish(TOPIC, message);

        // 메시지 수신을 위한 콜백 설정
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                fail("Connection lost: " + cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                assertEquals(TOPIC, topic); // 토픽이 올바른지 확인
                String payload = new String(message.getPayload());

                // JSON 객체 변환
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("topic", topic);
                jsonObject.put("data", new JSONObject(payload));

                // JSON 데이터 검증
                assertNotNull(jsonObject);
                assertEquals(2, jsonObject.getJSONObject("data").getInt("type"));
                assertEquals(14, jsonObject.getJSONObject("data").getInt("current"));

                System.out.println("Received JSON:");
                System.out.println(jsonObject.toString(4));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // 메시지 전달 완료 확인
                System.out.println("Message delivery complete: " + token.getMessageId());
            }
        });

        // 메시지 수신 대기 (비동기 방식으로 콜백을 기다리기 위함)
        Thread.sleep(5000);
    }
}
