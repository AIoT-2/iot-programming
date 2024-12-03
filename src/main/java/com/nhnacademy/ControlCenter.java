package com.nhnacademy;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;

public class ControlCenter {

    public static void main(String[] args) {
        final String host = "192.168.70.203"; // use your host-name, it should look like
        // '<alphanumeric>.s2.eu.hivemq.cloud'
        final String username = ""; // your credentials
        final String password = "";

        // 1. create the client
        final Mqtt5Client client = Mqtt5Client.builder()
                .identifier("controlcenter-1234")// + getMacAddress()) // use a unique identifier
                .serverHost(host)
                .automaticReconnectWithDefaultConfig() // the client automatically reconnects
                .serverPort(1883) // this is the port of your cluster, for mqtt it is the default port
                // 8883
                // .sslWithDefaultConfig() // establish a secured connection to HiveMQ Cloud
                // using TLS
                .build();

        // 2. connect the client
        client.toBlocking().connectWith()
                .simpleAuth() // u
                .username(username) // use the username and password you just createdsing authentication, which is required for a secure connection
                .password(password.getBytes(StandardCharsets.UTF_8))
                .applySimpleAuth()
                .cleanStart(false)
                .sessionExpiryInterval(TimeUnit.HOURS.toSeconds(1)) // buffer messages
                .send();

        // 3. subscribe and consume messages
        client.toAsync().subscribeWith()
                .topicFilter("data/#")
                .callback(publish -> {
                    System.out.println("Received message on topic " + publish.getTopic() + ": " +
                            new String(publish.getPayloadAsBytes(),
                                    StandardCharsets.UTF_8));
                })
                .send();
    }
}
