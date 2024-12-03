package com.nhnacademy.nhnacademy;

import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Sensor {

    public static void main(String[] args) throws InterruptedException {
        final String host = "tcp://127.0.0.1"; // use your host-name, it should look like '<alphanumeric>.s2.eu.hivemq.cloud'
        final String username = ""; // your credentials
        final String password = "";

        // 1. create the client
        final Mqtt5Client client = Mqtt5Client.builder()
                .identifier("sensor-02" )// getMacAddress()) // use a unique identifier
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
                .willPublish() // the last message, before the client disconnects
                .topic("home/will")
                .payload("sensor gone".getBytes())
                .applyWillPublish()
                .send();

        // 3. simulate periodic publishing of sensor data
        while (true) {
            client.toBlocking().publishWith()
                    .topic("home/brightness")
                    .payload(getBrightness())
                    .send();

            TimeUnit.MILLISECONDS.sleep(500);

            client.toBlocking().publishWith()
                    .topic("home/temperature")
                    .payload(getTemperature())
                    .send();

            TimeUnit.MILLISECONDS.sleep(500);
        }
    }
    //4. Simulate Temperature and Brightnes sensor data
    private static byte[] getBrightness() {
        // simulate a brightness sensor with values between 1000lux and 10000lux
        final int brightness = ThreadLocalRandom.current().nextInt(1_000, 10_000);
        return (brightness + "lux").getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] getTemperature() {
        // simulate a temperature sensor with values between 20°C and 30°C
        final int temperature = ThreadLocalRandom.current().nextInt(20, 30);
        return (temperature + "°C").getBytes(StandardCharsets.UTF_8);
    }
}
