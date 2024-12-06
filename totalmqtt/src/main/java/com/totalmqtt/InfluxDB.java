package com.totalmqtt;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;

import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
public class InfluxDB implements Runnable{
    private String mqttIp;
    private int mqttPort;
    private String username;
    private String password;
    private String control;
    private final Mqtt5Client client;
    private String influxDBIp;
    private int influxDBPort;
    // org Name (초기 설정에서 지정한 Organization Name)
    private static String ORG = "nhnacademy";
    // bucket name
    private static String BUCKET = "mqtt_modbus";
    // bucket Token
    private static char[] TOKEN = "Gksj8LnOLhrj4KJHPWza8OppXW1MO9TMpg8yElQZ1N4Lc3X3O4lbSyhSye58qkwPJ-K7bC2fXHDBJ2vKgaLS1g=="
            .toCharArray(); // 고치기

    public InfluxDB() {
        mqttIp = "192.168.71.205";
        mqttPort = 1883;
        influxDBIp = "192.168.71.205";
        influxDBPort = 8086;
        control = "controlcenter-1234";
        username = "";
        password = "";

        client = Mqtt5Client.builder()
                .identifier(control)
                .serverHost(mqttIp)
                .automaticReconnectWithDefaultConfig()
                .serverPort(mqttPort)
                .addDisconnectedListener(context -> handleDisconnect(context))
                .build();

    }

    public void mqttInformation(String ip, int port){
        this.mqttIp = ip;
        this.mqttPort = port;
    }

    public void influxDBInformation(String ip, int port){
        this.influxDBIp = ip;
        this.influxDBPort = port;
    }

    public void connect() {
        client.toBlocking().connectWith()
                .simpleAuth()
                .username(username)
                .password(password.getBytes(StandardCharsets.UTF_8))
                .applySimpleAuth()
                .cleanStart(false)
                .sessionExpiryInterval(TimeUnit.MINUTES.toSeconds(1))
                .send();
    }

    public void messageSend() {
        client.toAsync().subscribeWith() // toAsync : 비동기설정 // subscribeWith : 데이터를 가공하여 가져오기 위한 설정
                .topicFilter("application/#") // 모든 topic을 받아오겠다는 의미
                .callback(publish -> { // 메시지가 수신될 때 호출되는 콜백함수 정의
                    // 데이터가 수신되었는지 확인
                    try {
                        String sendAnwser = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
                        write(sendAnwser);

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

    private void write(String jsonData) {
        try {
            InfluxDBClient influxDBClient = InfluxDBClientFactory.create("http://"+influxDBIp+":"+influxDBPort, TOKEN, ORG, BUCKET);

            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            ObjectMapper objectMapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(jsonData, Map.class);

            // Point 객체 생성
            Point point = null;
            if(data.get("deviceName") == null){
                point = Point.measurement(data.get("name").toString())
                .time(System.currentTimeMillis(), WritePrecision.MS);
            }
            else{
                point = Point.measurement(data.get("deviceName").toString())
                .time(System.currentTimeMillis(), WritePrecision.MS);
            }

            // Map을 순회하여 필드를 추가
            point.addFields(data);

            writeApi.writePoint(point);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void run() {
        connect();
        messageSend();
        // while (!Thread.currentThread().isInterrupted()) {
            
        // }
    }
}
