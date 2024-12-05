package com.totalmqtt;

import java.util.Map;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

// 이름 바꾸기
@Slf4j
public class ToBroker  {
    // 클라이언트 ID
    private static final String CLIENT_ID = "";

    private String brokerIp;
    private int brokerPort;
    private MqttClient client;

    public ToBroker() {
        brokerIp = "192.168.71.205";
        brokerPort = 1883;
    }

    public void settingInformation(String ip, int port){
        brokerIp = ip;
        brokerPort = port;
    }

    public void connect(){
        try {
            client = new MqttClient("tcp://" + brokerIp + ":" + Integer.toString(brokerPort), CLIENT_ID);
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
                    // 데이터 삭제
                }
            });

            // 브로커 연결
            log.debug("Connecting to broker...");
            client.connect(options);
            log.debug("Connected!");
        } catch (MqttException e) {
            System.err.println(e.getMessage());
        }
    }

    public void send(Map<String, Object> data, String topic) throws InterruptedException {
        try{
            // ObjectMapper 객체 생성
            ObjectMapper objectMapper = new ObjectMapper();
            // publish
            String message = objectMapper.writeValueAsString(data);
            log.debug("Publishing message: " + message);
            client.publish(topic, new MqttMessage(message.getBytes()));

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
