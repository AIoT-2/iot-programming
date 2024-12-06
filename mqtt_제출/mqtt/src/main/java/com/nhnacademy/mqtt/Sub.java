package com.nhnacademy.mqtt;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.config.InfluxdbConfig;
import com.nhnacademy.config.MqttConfig;
import com.nhnacademy.influxdb.InfluxDBWriter;
import com.nhnacademy.utils.TopicParser;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Sub implements Runnable {
    private MqttConfig mqttConfig;
    private InfluxDBWriter influxDBWriter;

    public Sub(MqttConfig mqttConfig, InfluxdbConfig influxdbConfig) {
        this.mqttConfig = mqttConfig;
        influxDBWriter = new InfluxDBWriter(influxdbConfig);
    }

    public void subscribe() {
        try (MqttClient client = new MqttClient(mqttConfig.getBroker(),
                mqttConfig.getClientId() + "-" + UUID.randomUUID().toString())) {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            String[] topics = mqttConfig.getTopics().toArray(new String[0]);
            int[] qos = new int[topics.length];
            Arrays.fill(qos, mqttConfig.getQos());

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    handleConnectionLost(client, options, cause, topics, qos);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    handleMessageArrival(topic, message);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    log.debug("Message delivery complete: {}", token.getMessageId());
                }
            });

            log.debug("Connecting to broker...");
            client.connect(options);
            log.info("Connected!");

            client.subscribe(topics, qos);
            log.info("Subscribed to topics: {}", String.join(", ", mqttConfig.getTopics()));
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void handleConnectionLost(MqttClient client, MqttConnectOptions options,
            Throwable cause, String[] topics, int[] qos) {
        log.trace("Connection lost: {}", cause.getMessage());
        // 자동 재연결 시도
        while (!client.isConnected()) {
            try {
                log.info("Reconnecting...");
                client.connect(options);
                log.info("Reconnected!");
                client.subscribe(topics, qos);
            } catch (MqttException e) {
                log.info("Reconnection failed: {}", e.getMessage());
                try {
                    Thread.sleep(5000); // 5초 후 재시도
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void handleMessageArrival(String topic, MqttMessage message) {
        if (topic.contains("lora") || topic.contains("power_meter")) {
            return;
        }

        log.info("Received message from topic '{}': {}", topic, new String(message.getPayload()));

        try {
            // MQTT 메시지 페이로드를 JSON으로 파싱
            String payload = new String(message.getPayload());
            JsonNode jsonNode = new ObjectMapper().readTree(payload);

            // time과 value 값을 추출
            long timeMillis = jsonNode.get("time").asLong();
            double value = jsonNode.get("value").asDouble();

            // 토픽을 '/'로 분리하여 map으로 변환 태그로 사용
            Map<String, String> topicMap = TopicParser.parse(topic);

            // bucketToUse를 해당 토픽에 따라 결정
            String pointMeasure = determinePointMeasure(topic);

            // InfluxDB에 데이터 저장
            influxDBWriter.writeToInfluxDB(topicMap, pointMeasure, timeMillis, value);
        } catch (Exception e) {
            log.debug("Error processing message: {}", e.getMessage());
        }
    }

    private String determinePointMeasure(String topic) {
        if (topic.startsWith("dongdong/")) {
            return "sensor_data";
        } else if (topic.startsWith("data/")) {
            return "room_data";
        }
        return "default_measure"; // 기본값 또는 에러 처리
    }

    @Override
    public void run() {
        subscribe();
    }
}
