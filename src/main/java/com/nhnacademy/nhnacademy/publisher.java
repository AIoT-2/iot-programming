package com.nhnacademy.nhnacademy;

import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class publisher {

        public static void main(String[] args) {
            final String host = "<your_host>"; // use your host-name, it should look like '<alphanumeric>.s2.eu.hivemq.cloud'
            final String username = "<your_username>";  // your credentials
            final String password = "<your_password>";

            // 1. create the client
            final Mqtt5Client client = Mqtt5Client.builder()
                    .identifier("controlcenter-23") // use a unique identifier
                    .serverHost(host)
                    .automaticReconnectWithDefaultConfig() // the client automatically reconnects
                    .serverPort(8883) // this is the port of your cluster, for mqtt it is the default port 8883
                    .sslWithDefaultConfig() // establish a secured connection to HiveMQ Cloud using TLS
                    .build();

            // 2. connect the client
            client.toBlocking().connectWith()
                    .simpleAuth() // using authentication, which is required for a secure connection
                    .username(username) // use the username and password you just created
                    .password(password.getBytes(StandardCharsets.UTF_8))
                    .applySimpleAuth()
                    .cleanStart(false)
                    .sessionExpiryInterval(TimeUnit.HOURS.toSeconds(1)) // buffer messages
                    .send();

            // 3. subscribe and consume messages
            client.toAsync().subscribeWith()
                    .topicFilter("home/#")
                    .callback(publish -> {
                        System.out.println("Received message on topic " + publish.getTopic() + ": " +
                                new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8));
                    })
                    .send();
        }

}
