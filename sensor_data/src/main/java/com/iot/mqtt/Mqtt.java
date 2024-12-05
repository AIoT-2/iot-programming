package com.iot.mqtt;

import java.time.Instant;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.write.Point;
import com.influxdb.client.domain.WritePrecision;

public class Mqtt extends MqttTransform {
    static final Logger logger = LoggerFactory.getLogger(Mqtt.class);

    private static final String BROKER = "tcp://192.168.70.203:1883";
    private static final String CLIENT_ID = "song";
    private static final String TOPIC = "data/#";
    private static MqttToDB mqttToDB = new MqttToDB();

    public static void main(String[] args) throws InterruptedException {

        try (MqttClient client = new MqttClient(BROKER, CLIENT_ID)) {

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            // 메시지 수신 콜백 설정
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Connection lost: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {

                    String payload = new String(message.getPayload());
                    ObjectMapper objectMapper = new ObjectMapper();
                    String measurement = extractPlace(topic);

                    try {
                        JsonNode jsonNode = objectMapper.readTree(payload);

                        JsonNode valueNode = jsonNode.get("value");

                        if (extractElement(topic).equals("lora") || extractElement(topic).equals("power-meter")
                                || extractElement(topic).equals("di")) {
                            return;
                        }

                        Point pointBuilder = Point.measurement(measurement)
                                .addTag("spot", extractName(topic))
                                .addTag("value", extractElement(topic))
                                .addField("payload", valueNode.asDouble())
                                .time(Instant.now(), WritePrecision.NS);

                        mqttToDB.writeToDB(pointBuilder);

                        logger.debug("Field: {}", extractName(topic));
                        logger.debug("Measurement: {}", extractPlace(topic));
                        logger.debug("Value: {}", extractElement(topic));
                        logger.debug("Topic: {}", topic);
                        logger.debug("msg: {}", valueNode.asDouble());
                        System.out.println();

                    } catch (Exception e) {
                        System.out.println("Invalid JSON payload: ");
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("Message delivery complete: "
                            + token.getMessageId());
                }
            });

            try {
                logger.info("Connecting to broker...");
                client.connect(options);
                logger.info("Connected!");

                logger.info("Subscribing to topic: {}", TOPIC);
                client.subscribe(TOPIC);

                while (client.isConnected()) {
                    Thread.sleep(1000);
                }

            } catch (MqttException | InterruptedException e) {
                logger.error("Error in MQTT client: {}", e.getMessage());
                Thread.sleep(5000); // 연결 실패 시 재시도 간격
            } finally {
                if (client.isConnected()) {
                    client.disconnect();
                }
                logger.info("Disconnected from broker.");
            }

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}