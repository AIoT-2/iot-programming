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

public class Mqtt extends MqttTransform implements Runnable {
    static final Logger logger = LoggerFactory.getLogger(Mqtt.class);

    private static final String BROKER = "tcp://192.168.70.203:1883";
    private static final String CLIENT_ID = "kim";
    private static final String TOPIC = "007/data";
    private static MqttToDB mqttToDB = new MqttToDB();

    @Override
    public void run() {

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

                    System.out.println("Received message from topic '"
                            + topic + "': " + new String(message.getPayload()));

                    String payload = new String(message.getPayload());
                    ObjectMapper objectMapper = new ObjectMapper();
                    String measurement = extractPlace(topic);
                    logger.debug("ppppppppppppppppp: ", payload);
                    logger.debug("MMMMMMMMMMMMMMMM: ", measurement);

                    if (measurement == null) {
                        logger.debug("Measurement is null, using default measurement");
                        measurement = "default_measurement";
                    }

                    try {
                        JsonNode jsonNode = objectMapper.readTree(payload);
                        JsonNode valueNode = jsonNode.get("value");

                        // String element = extractElement(topic);

                        // if (element.equals("lora") || element.equals("power-meter")
                        // || element.equals("di")) {
                        // return;
                        // }

                        // valueNode가 null인 경우에 대비하여 안전한 처리
                        if (valueNode == null || valueNode.isNull()) {
                            logger.warn("valueNode is null or missing in the payload");
                            return; // valueNode가 없으면 메시지 처리를 중지
                        }

                        Point pointBuilder = Point.measurement(measurement)
                                .addTag("spot", extractName(topic))
                                .addTag("value", extractElement(topic))
                                .addField("payload", valueNode.asDouble())
                                .time(Instant.now(), WritePrecision.NS);

                        // mqttToDB.writeToDB(pointBuilder);

                        logger.debug("Field: {}", extractName(topic));
                        logger.debug("Measurement: {}", extractPlace(topic));
                        logger.debug("Value: {}", extractElement(topic));
                        logger.debug("Topic: {}", topic);
                        logger.debug("msg: {}", valueNode.asDouble());
                        System.out.println();

                    } catch (Exception e) {
                        logger.debug("Invalid JSON payload: ", e);
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
                logger.info("Connected to MQTT broker: " + BROKER);

                if (client.isConnected()) {
                    client.subscribe(TOPIC);
                    logger.info("Successfully subscribed to topic: " + TOPIC);
                } else {
                    logger.error("MQTT client failed to connect to broker");
                }

                logger.info("Subscribing to topic: {}", TOPIC);
                client.subscribe(TOPIC);

                while (client.isConnected()) {
                    Thread.sleep(1000);
                }

            } catch (MqttException | InterruptedException e) {
                logger.error("Error in MQTT client: {}", e.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                } // 연결 실패 시 재시도 간격
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

    public static void main(String[] args) {
        Thread mqttThread = new Thread(new Mqtt());
        mqttThread.start();
    }
}