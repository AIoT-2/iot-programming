package com.nhnacademy.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.client.mqtt.datatypes.MqttTopic;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MqttPublisher implements Runnable {

    static final String DEFAULT_PUBLISH_HOST = "tcp://localhost:1883";
    static final String DEFAULT_PUBLISH_USERNAME = "";
    static final String DEFAULT_PUBLISH_PASSWORD = "";
    static final String DEFAULT_PUBLISH_TOPIC = "sensor/data";

    final String publishHost;  // 송신 브로커 IP
    final String publishUsername; // 수신 브로커 사용자 이름
    final String publishPassword;
    final String publishTopic;
    final Mqtt5Client publisher;
    final String message;

    public MqttPublisher(String message) {
        this(DEFAULT_PUBLISH_HOST, DEFAULT_PUBLISH_USERNAME, DEFAULT_PUBLISH_PASSWORD, DEFAULT_PUBLISH_TOPIC, message);
    }

    public MqttPublisher(String publishHost, String publishUsername, String publishPassword, String publishTopic, String message) {
        this.publishHost = publishHost;
        this.publishUsername = publishUsername;
        this.publishPassword = publishPassword;
        this.publishTopic = publishTopic;
        this.publisher = createClient();
        this.message = message;
    }

    public Mqtt5Client createClient() {
        return Mqtt5Client.builder()
                .identifier("sub" + UUID.randomUUID())
                .serverHost(publishHost)
                .automaticReconnectWithDefaultConfig()
                .serverPort(1883) // MQTT 기본 포트
                .build();
    }

    public void connect() {
        publisher.toBlocking().connectWith()
                .simpleAuth()
                .username(publishUsername)
                .password(publishPassword.getBytes(StandardCharsets.UTF_8))
                .applySimpleAuth()
                .cleanStart(false)
                .sessionExpiryInterval(TimeUnit.HOURS.toSeconds(1))
                .send();
    }

    // 받은 Message 가공 후 전송
    public JSONObject transformer() {
        JSONObject mqttMessage;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(message);


            String deviceName = rootNode.path("deviceInfo").path("deviceName").asText();
            String spotName = rootNode.path("deviceInfo").path("tags").path("place").asText();
            JsonNode objectNode = rootNode.path("object");
            if (!objectNode.isMissingNode()) {
                log.info("Object Data: {}", objectNode);
            }

            // 'tags' 추가
            mqttMessage = new JSONObject();
            JSONObject mqttTags = new JSONObject();
            mqttTags.put("deviceId", deviceName);
            mqttTags.put("location", spotName);
            mqttMessage.put("tags", mqttTags);

            // `data` 필드 추가
            mqttMessage.put("data", new JSONObject(objectNode.toString()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return mqttMessage;
    }

    public void publish() {
        JSONObject mqttMessage = transformer();
        Mqtt5Publish publishMessage = Mqtt5Publish.builder()
                .topic(MqttTopic.of(publishTopic))
                .payload(mqttMessage.toString().getBytes(StandardCharsets.UTF_8))
                .build();
        publisher.toAsync().publish(publishMessage).whenComplete((publishResult, throwable) -> {
            if (throwable != null) {
                log.info("Failed to publish message:{} ", throwable.getMessage());
            } else {
                log.info("Published processed message to local broker: {}", publishMessage);
            }
        });

    }

    @Override
    public void run() {
        connect();
        publish();
    }
}
