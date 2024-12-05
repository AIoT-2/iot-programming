package com.example.mqtt_mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.paho.client.mqttv3.*;

@Slf4j
public class MqttSubscriber {
    private static final String BROKER = "tcp://192.168.70.203:1883";
    private static final String CLIENT_ID = "JavaSubscriber";
    private static final String SUBSCRIBE_TOPIC = "data/#";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    private MqttPublisher publisher;

    public MqttSubscriber(MqttPublisher publisher) {
        this.publisher = publisher;
    }

    public void start() {
        try (MqttClient client = new MqttClient(BROKER, CLIENT_ID)) {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.debug("Connection lost: {}", cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload = new String(message.getPayload());
                    log.debug("Received msg topic : {}", topic);
                    log.debug("Received msg payload: {}", payload);

                    
                    JsonNode rootNode = MAPPER.readTree(payload);
                    String columns = topic.substring(topic.lastIndexOf("/") + 1);
                    double value = rootNode.path("value").asDouble();
                    long time = rootNode.path("time").asLong();
                    String tag = rootNode.path("deviceInfo").path("deviceName").asText("UnknownDevice");

                    // TransformedData 객체 생성
                    TransformedData transformedData = new TransformedData(columns, value, time, tag);

                    // Publisher를 통해 발행
                    String transformedMessage = MAPPER.writeValueAsString(transformedData);
                    publisher.publish(columns, transformedMessage);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    log.debug("Message delivery complete: {}", token.getMessageId());
                }
            });

            log.info("Connecting to broker...");
            client.connect(options);
            log.info("Connected!");

            log.info("Subscribing to topic: {}", SUBSCRIBE_TOPIC);
            client.subscribe(SUBSCRIBE_TOPIC);

            Thread.sleep(100000);

            client.disconnect();
            log.info("Disconnected!");
        } catch (MqttException | InterruptedException e) {
            log.debug("ErrorMessage: {}", e.getMessage());
        }
    }
}
