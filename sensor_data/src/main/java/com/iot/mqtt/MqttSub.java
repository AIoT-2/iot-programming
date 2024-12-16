package com.iot.mqtt;

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

public class MqttSub extends MqttTransform implements Runnable {
    static final Logger logger = LoggerFactory.getLogger(MqttSub.class);

    private static final String BROKER = "tcp://192.168.70.203:1883";
    private static final String CLIENT_ID = "songs";
    private static final String TOPIC = "songs/#";
    // private static final String TOPIC2 = "data/#";
    private static MqttToDB mqttToDB = new MqttToDB();

    @Override
    public void run() {

        try (MqttClient client = new MqttClient(BROKER, CLIENT_ID)) {

            MqttConnectOptions options = new MqttConnectOptions();
            options.setKeepAliveInterval(60); // Seconds
            options.setAutomaticReconnect(true);
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

                    try {
                        JsonNode jsonNode = objectMapper.readTree(payload);
                        JsonNode valueNode = jsonNode.get("value");

                        // valueNode가 null인 경우에 대비하여 안전한 처리
                        if (valueNode == null || valueNode.isNull()) {
                            logger.warn("valueNode is null or missing in the payload");
                            return;
                        }

                        // MySQL에 데이터 삽입
                        mqttToDB.insertIntoMySQL(topic, payload);

                        logger.debug("Topic: {}", topic);
                        logger.debug("Payload: {}", payload);
                        System.out.println();

                    } catch (Exception e) {
                        logger.debug("Invalid JSON payload: ", e);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }

            });

            try {
                logger.info("Connecting to broker...");
                client.connect(options);
                logger.info("Connected to MQTT broker: " + BROKER);

                if (client.isConnected()) {
                    client.subscribe(TOPIC);
                    // client.subscribe(TOPIC2);
                    logger.info("Successfully subscribed to topic: " + TOPIC);
                    // logger.info("Successfully subscribed to topic2: " + TOPIC2);
                } else {
                    logger.error("MQTT client failed to connect to broker");
                }

                while (client.isConnected()) {
                    run();
                    Thread.sleep(5000);
                }

            } catch (MqttException | InterruptedException e) {
                logger.error("Error in MQTT client: {}", e.getMessage());
                try {
                    Thread.sleep(10000);
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
        Thread mqttSubThread = new Thread(new MqttSub());
        mqttSubThread.start();
    }
}