package com.nhnacademy;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.paho.client.mqttv3.*;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.write.Point;
import com.influxdb.client.domain.WritePrecision;

public class MQTTTest {
    private static final String BROKER = "tcp://192.168.70.203:1883";
    private static final String CLIENT_ID = "Seong";
    private static final String TOPIC = "data/#";

    private static final String URL = "http://192.168.71.221:8086";
    private static final String ORG = "seong";
    private static final String BUCKET = "tests";
    private static final char[] TOKEN = "kBZS4KT4kTDB93jvwi9EijocQ_zWSWqw-ebKaId_mXaVgauLuttoWCEWqCtr6DUlKTcwASgjxip09SGg3xDnpQ=="
            .toCharArray();

    private final InfluxDBClient influxDBClient;
    private final WriteApiBlocking writeApiBlocking;

    public MQTTTest() {
        this.influxDBClient = InfluxDBClientFactory.create(URL, TOKEN, ORG, BUCKET);
        this.writeApiBlocking = influxDBClient.getWriteApiBlocking();
    }

    // public static void main(String[] args) {
    // MQTTTest app = new MQTTTest();
    // app.start();
    // }

    public void start() {
        try (MqttClient client = new MqttClient(BROKER, CLIENT_ID)) {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.err.println("Connection lost: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    handleMessage(topic, message);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("Delivery complete: " + token.getMessageId());
                }
            });

            System.out.println("Connecting to broker...");
            client.connect(options);
            System.out.println("Connected!");

            System.out.println("Subscribing to topic: " + TOPIC);
            client.subscribe(TOPIC);

            // 계속 실행
            while (true) {
                TimeUnit.SECONDS.sleep(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 메시지를 처리하여 InfluxDB에 저장하는 메서드.
     */
    private void handleMessage(String topic, MqttMessage message) {
        try {
            System.out.println("Received message from topic: " + topic);
            String payload = new String(message.getPayload());
            System.out.println("Payload: " + payload);

            // 'lora' 데이터 제외
            if (topic.contains("lora")) {
                System.out.println("Lora data received, skipping...");
                return; // lora 관련 데이터는 제외
            }

            // JSON 파싱
            JsonObject jsonPayload = JsonParser.parseString(payload).getAsJsonObject();
            JsonElement valueElement = jsonPayload.get("value");
            if (valueElement == null) {
                System.err.println("Invalid message: no 'value' field found.");
                return;
            }

            // 주제 분석
            String measurement = extractSegment(topic, "/e/", "/");
            String placeName = extractSegment(topic, "/p/", "/d/");
            String deviceName = extractSegment(topic, "/n/", "/e/");

            // Point 객체 생성
            Point point = Point.measurement(measurement)
                    .addTag("deviceName", deviceName)
                    .addTag("placeName", placeName)
                    .time(Instant.now(), WritePrecision.MS);

            // value 타입에 따른 처리
            if (valueElement.isJsonPrimitive()) {
                if (valueElement.getAsJsonPrimitive().isNumber()) {
                    point.addField("value", valueElement.getAsDouble());
                } else if (valueElement.getAsJsonPrimitive().isBoolean()) {
                    point.addField("value", valueElement.getAsBoolean());
                } else if (valueElement.getAsJsonPrimitive().isString()) {
                    point.addField("value", valueElement.getAsString());
                }
            } else if (valueElement.isJsonObject()) {
                // JSON 객체를 문자열로 저장
                point.addField("value", valueElement.toString());
            } else {
                System.err.println("Unsupported value type: " + valueElement);
                return;
            }

            // 데이터 쓰기
            writeApiBlocking.writePoint(point);
            System.out.println("Data written to InfluxDB: " + point);
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 주제에서 특정 세그먼트를 추출하는 메서드.
     *
     * @param topic          주제
     * @param startDelimiter 시작 구분자
     * @param endDelimiter   끝 구분자
     * @return 추출된 세그먼트
     */
    private String extractSegment(String topic, String startDelimiter, String endDelimiter) {
        int startIndex = topic.indexOf(startDelimiter) + startDelimiter.length();
        int endIndex = topic.indexOf(endDelimiter, startIndex);
        if (endIndex == -1) {
            endIndex = topic.length();
        }
        return topic.substring(startIndex, endIndex);
    }
}
