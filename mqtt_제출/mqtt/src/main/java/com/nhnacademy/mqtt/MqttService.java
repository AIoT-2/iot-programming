package com.nhnacademy.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MqttService {
    private MqttClient client;
    private final String broker;
    private final String clientId;

    public MqttService(String broker, String clientId) throws MqttException {
        this.broker = broker;
        this.clientId = clientId;
        this.client = new MqttClient(broker, clientId);
    }

    public void connect(MqttConnectOptions options) throws MqttException {
        client.connect(options);
        log.info(clientId + " connected to broker: " + broker);
    }

    public void disconnect() throws MqttException {
        client.disconnect();
        log.info(clientId + " disconnected from broker.");
    }

    public MqttClient getClient() {
        return client;
    }
}
