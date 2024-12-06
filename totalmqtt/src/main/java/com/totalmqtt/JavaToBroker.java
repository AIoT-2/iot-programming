package com.totalmqtt;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JavaToBroker implements Runnable, Consumer{
    private String mqttIp;
    private int mqttPort;
    private String username;
    private String password;
    private String control;
    private Mqtt5Client client;

    private InfluxDB influxDB;

    public JavaToBroker(){
        mqttIp = "192.168.71.205";
        mqttPort = 1883;
        control = "controlcenter-1234";
        username = "";
        password = "";
    }

    public void settingInformation(String ip, int port){
        this.mqttIp = ip;
        this.mqttPort = port;
    }

    public void setInfluxDB(InfluxDB influxDB){
        this.influxDB = influxDB;
    }

    private void handleDisconnect(MqttClientDisconnectedContext context) {
        Throwable cause = context.getCause();
        if (cause != null) {
            log.debug("Disconnected! Reason: " + cause.getMessage()); // 연결끊김의 원인을 파악함
        } else {
            log.debug("Disconnected gracefully.");
        }

        context.getReconnector() // 재연결 작업을 처리
                .reconnect(true) // 자동 재연결 활성화
                .delay(5, TimeUnit.SECONDS); // 재연결 전에 5초 지연
    }

    public void connect() {
        client = Mqtt5Client.builder()
                .identifier(control)
                .serverHost(mqttIp)
                .automaticReconnectWithDefaultConfig()
                .serverPort(mqttPort)
                .addDisconnectedListener(context -> handleDisconnect(context))
                .build();

        client.toBlocking().connectWith()
                .simpleAuth()
                .username(username)
                .password(password.getBytes(StandardCharsets.UTF_8))
                .applySimpleAuth()
                .cleanStart(false)
                .sessionExpiryInterval(TimeUnit.MINUTES.toSeconds(1))
                .send();
    }

    @Override
    public void run() {
        execute("");
    }

    @Override
    public void execute(String jsonData) {
        client.toAsync().subscribeWith() // toAsync : 비동기설정 // subscribeWith : 데이터를 가공하여 가져오기 위한 설정
                .topicFilter("application/#") // 모든 topic을 받아오겠다는 의미
                .callback(publish -> { // 메시지가 수신될 때 호출되는 콜백함수 정의
                    // 데이터가 수신되었는지 확인
                    try {
                        String sendAnwser = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
                        //influxdb에 연결
                        influxDB.execute(sendAnwser);
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                    }
                })
                .send()
                .whenComplete((subAck, throwable) -> { // subscribe가 broker에 연결되었는지 확인
                    if (throwable != null)
                        System.err.println("Failed to subscribe: " + throwable.getMessage());
                    else {
                        log.debug("Subscribed successfully!");
                    }
                });
    }
}
