package com.example;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MQTTSubscriber {
    private static final String SOURCE_BROKER = "tcp://192.168.70.203:1883";
    private static final String DEST_BROKER = "tcp://localhost:1883";
    private static final String SOURCE_CLIENT_ID = "inho_source";
    private static final String DEST_CLIENT_ID = "inho_dest";
    private static final String SOURCE_TOPIC = "application/#";
    private static final String DEST_TOPIC_PREFIX = "sensor";

    private MqttClient sourceClient;
    private MqttClient destClient;
    private MqttConnectOptions options;

    public MQTTSubscriber() throws MqttException {
        initializeConnections();
    }

    private void initializeConnections() throws MqttException {
        sourceClient = new MqttClient(SOURCE_BROKER, SOURCE_CLIENT_ID);
        destClient = new MqttClient(DEST_BROKER, DEST_CLIENT_ID);
        setupConnectionOptions();
        connect();
    }

    private void setupConnectionOptions() {
        options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        options.setKeepAliveInterval(30);
        options.setConnectionTimeout(60);
    }

    private void connect() throws MqttException {
        try {
            sourceClient.connect(options);
            destClient.connect(options);
            setupCallbacks();
            sourceClient.subscribe(SOURCE_TOPIC);
            System.out.println("Connected to brokers and subscribed to: " + SOURCE_TOPIC);
        } catch (MqttException e) {
            System.err.println("Connection failed: " + e.getMessage());
            throw e;
        }
    }

    private void setupCallbacks() {
        sourceClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                System.out.println("Source broker connection lost: " + cause.getMessage());
                reconnect();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                handleMessage(topic, message);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });
    }

    private void reconnect() {
        Thread reconnectThread = new Thread(() -> {
            while (!sourceClient.isConnected() || !destClient.isConnected()) {
                try {
                    System.out.println("Attempting to reconnect...");
                    Thread.sleep(5000);  // 5초 대기

                    if (!sourceClient.isConnected()) {
                        sourceClient.connect(options);
                        sourceClient.subscribe(SOURCE_TOPIC);
                        System.out.println("Reconnected to source broker");
                    }

                    if (!destClient.isConnected()) {
                        destClient.connect(options);
                        System.out.println("Reconnected to destination broker");
                    }

                } catch (Exception e) {
                    System.err.println("Reconnection attempt failed: " + e.getMessage());
                }
            }
        });
        reconnectThread.setDaemon(true);
        reconnectThread.start();
    }

    private void handleMessage(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload());
            System.out.println("Received from source - Topic: " + topic);
            System.out.println("Payload: " + payload);

            // 대상 브로커로 전송
            MqttMessage newMessage = new MqttMessage(payload.getBytes());
            newMessage.setQos(1);
            String destTopic = DEST_TOPIC_PREFIX + "/" + topic;
            destClient.publish(destTopic, newMessage);
            System.out.println("Published to destination broker");

        } catch (Exception e) {
            System.err.println("Error handling message: " + e.getMessage());
        }
    }

    public void close() {
        try {
            if (sourceClient.isConnected()) sourceClient.disconnect();
            if (destClient.isConnected()) destClient.disconnect();
            System.out.println("Disconnected from brokers");
        } catch (MqttException e) {
            System.err.println("Error closing connections: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            MQTTSubscriber subscriber = new MQTTSubscriber();

            Runtime.getRuntime().addShutdownHook(new Thread(subscriber::close));

            // 메인 스레드 유지
            while (true) {
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}