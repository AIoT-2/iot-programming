package com.nhnacademy.mqtt;

import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
@Slf4j
public class MqttSubscriber implements Runnable{
    private static final String DEFAULT_RECEIVE_HOST = "192.168.70.203";
    private static final String DEFAULT_RECEIVE_USERNAME = "";
    private static final String DEFAULT_RECEIVE_PASSWORD = "";
    private static final String DEFAULT_RECEIVE_TOPIC = "application/#";

    private final String receiveHost;  // 수신 브로커 IP
    private final String receiveUsername; // 수신 브로커 사용자 이름
    private final String receivePassword;
    private final String receiveTopic;
    private final Mqtt5Client subscriber;


    public MqttSubscriber(String receiveHost, String receiveUsername, String receivePassword, String receiveTopic) {
        this.receiveHost = receiveHost;
        this.receiveUsername = receiveUsername;
        this.receivePassword = receivePassword;
        this.receiveTopic = receiveTopic;
        this.subscriber = createClient();
    }

    public MqttSubscriber(){
        this(DEFAULT_RECEIVE_HOST,DEFAULT_RECEIVE_USERNAME,DEFAULT_RECEIVE_PASSWORD,DEFAULT_RECEIVE_TOPIC);
    }
    public Mqtt5Client createClient(){
        return Mqtt5Client.builder()
                .identifier("sub"+UUID.randomUUID())
                .serverHost(receiveHost)
                .automaticReconnectWithDefaultConfig()
                .serverPort(1883) // MQTT 기본 포트
                .build();
    }

    public void connect() {
        subscriber.toBlocking().connectWith()
                .simpleAuth()
                .username(receiveUsername)
                .password(receivePassword.getBytes(StandardCharsets.UTF_8))
                .applySimpleAuth()
                .cleanStart(false)
                .sessionExpiryInterval(TimeUnit.HOURS.toSeconds(1))
                .send();
        log.info("connected to {}", receiveHost);
    }

    public void subscribing() {

        subscriber.toAsync().subscribeWith()
                .topicFilter(receiveTopic)
                // 수신 브로커의 주제
                .callback(publish -> {
                    String message = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
                    connectPublisher(message);

                })
                .send();

    }

    private void connectPublisher(String message) {
        String host = "127.0.0.1";
        String publishTopic ="sensor/data";
        MqttPublisher mqttPublisher = new MqttPublisher(host,"","",publishTopic,message);
        Thread thread = new Thread(mqttPublisher);
        thread.start();
    }

    public void run() {
        connect();
        subscribing();
    }

}

