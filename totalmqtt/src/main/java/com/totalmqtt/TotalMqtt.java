package com.totalmqtt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class TotalMqtt {
    private String ip;
    private int port;
    private String username;
    private String password;
    private String control;
    private final Mqtt5Client client;

    private static String url = "http://192.168.71.205:8086";
    // org Name (초기 설정에서 지정한 Organization Name)
    private static String org = "nhnacademy";
    // bucket name
    private static String bucket = "mqtt_modbus";
    // bucket Token
    private static char[] token = "Gksj8LnOLhrj4KJHPWza8OppXW1MO9TMpg8yElQZ1N4Lc3X3O4lbSyhSye58qkwPJ-K7bC2fXHDBJ2vKgaLS1g=="
            .toCharArray();

    public TotalMqtt() {
        ip = "192.168.71.205";
        port = 1883;
        control = "controlcenter-1234";
        username = "";
        password = "";

        client = Mqtt5Client.builder()
                .identifier(control)
                .serverHost(ip)
                .automaticReconnectWithDefaultConfig()
                .serverPort(port)
                .addDisconnectedListener(context -> handleDisconnect(context))
                .build();

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

    public void MSGSend() {
        client.toAsync().subscribeWith() // toAsync : 비동기설정 // subscribeWith : 데이터를 가공하여 가져오기 위한 설정
                .topicFilter("application/#") // 모든 topic을 받아오겠다는 의미
                .callback(publish -> { // 메시지가 수신될 때 호출되는 콜백함수 정의
                    // 데이터가 수신되었는지 확인
                    try {
                        write(new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8));

                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                    }
                })
                .send()
                .whenComplete((subAck, throwable) -> { // subscribe가 broker에 연결되었는지 확인
                    if (throwable != null)
                        System.err.println("Failed to subscribe: " + throwable.getMessage());
                    else {
                        System.out.println("Subscribed successfully!");
                    }
                });
    }

    private void handleDisconnect(MqttClientDisconnectedContext context) {
        Throwable cause = context.getCause();
        if (cause != null) {
            System.err.println("Disconnected! Reason: " + cause.getMessage()); // 연결끊김의 원인을 파악함
        } else {
            System.out.println("Disconnected gracefully.");
        }

        context.getReconnector() // 재연결 작업을 처리
                .reconnect(true) // 자동 재연결 활성화
                .delay(5, TimeUnit.SECONDS); // 재연결 전에 5초 지연
    }

    public void write(String jsonData) {
        try {
            InfluxDBClient influxDBClient = InfluxDBClientFactory.create("http://192.168.71.205:8086", token, org, bucket);

            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> data = objectMapper.readValue(jsonData, Map.class);

            // Point 객체 생성
            Point point = Point.measurement(data.get("name").toString())
                    .addField("temperature", 22.5) // 예시 필드 값 추가
                    .time(System.currentTimeMillis(), WritePrecision.MS);

            // Map을 순회하여 필드를 추가
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                Object value = entry.getValue();

                // 각 값의 타입에 맞게 addField를 호출
                if (value instanceof Boolean) {
                    point.addField(entry.getKey(), (Boolean) value);
                } else if (value instanceof Integer) {
                    point.addField(entry.getKey(), (Integer) value);
                } else if (value instanceof Long) {
                    point.addField(entry.getKey(), (Long) value);
                } else if (value instanceof Double) {
                    point.addField(entry.getKey(), (Double) value);
                } else if (value instanceof Float) {
                    point.addField(entry.getKey(), (Float) value);
                } else if (value instanceof String) {
                    point.addField(entry.getKey(), (String) value);
                } else {
                    // 타입이 지원되지 않는 경우
                    System.out.println("Unsupported data type: " + value.getClass().getName());
                }
            }

            writeApi.writePoint(point);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
