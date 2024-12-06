package com.totalmqtt;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;

import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
public class MqttToData implements Producer {
    private String ip;
    private int port;
    private String username;
    private String password;
    private String control;
    private final Mqtt5Client client;

    public MqttToData() {
        ip = "192.168.70.203";
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
                // .addDisconnectedListener(this::handleDisconnect)
                .build();
    }

    public void settingInformation(String ip, int port) {
        this.ip = ip;
        this.port = port;
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

    private Map<String, Object> getData(JsonNode data) {
        Map<String, Object> totalData = new HashMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = data.fields(); it.hasNext();) {
            Map.Entry<String, JsonNode> field = it.next();
            String fieldName = field.getKey();
            JsonNode fieldValue = field.getValue();
            totalData.put(fieldName, fieldValue);
        }
        return totalData;
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

    @Override
    public void execute(int offset) {
        client.toAsync().subscribeWith() // toAsync : 비동기설정 // subscribeWith : 데이터를 가공하여 가져오기 위한 설정
                .topicFilter("application/#") // 모든 topic을 받아오겠다는 의미
                .callback(publish -> { // 메시지가 수신될 때 호출되는 콜백함수 정의
                    // 데이터가 수신되었는지 확인
                    String msgData = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
                    // Jackson ObjectMapper 생성
                    ObjectMapper mapper = new ObjectMapper();

                    try {
                        // MQTT 메시지를 JsonNode로 변환
                        JsonNode payload = mapper.readTree(msgData);

                        // JsonNode 데이터 출력
                        JsonNode deviceInfo = payload.path("deviceInfo");
                        JsonNode tags = deviceInfo.path("tags");
                        JsonNode object = payload.path("object");

                        log.debug("Json Topic : ");
                        String topic = publish.getTopic().toString();

                        Map<String, Object> totalData = new HashMap<>();

                        totalData.putAll(getData(deviceInfo));
                        totalData.putAll(getData(tags));
                        totalData.putAll(getData(object));
                        totalData.remove("tags");

                        log.debug("TotalData : ");
                        log.debug(totalData.toString());

                        ToBroker toBroker = new ToBroker();
                        topic = "application/" + totalData.get("deviceName");
                        toBroker.connect();
                        toBroker.send(totalData, topic);
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                    }
                })
                .send()
                .whenComplete((subAck, throwable) -> { // subscribe가 broker에 연결되었는지 확인
                    if (throwable != null)
                        log.debug("Failed to subscribe: " + throwable.getMessage());
                    else {
                        log.debug("Subscribed successfully!");
                    }
                });
    }
}
