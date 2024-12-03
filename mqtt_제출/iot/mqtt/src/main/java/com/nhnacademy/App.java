package com.nhnacademy;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;

public class App {
    private static final String BROKER = "tcp://192.168.70.203:1883";
    private static final String CLIENT_ID = "dongdong";
    private static final String[] TOPICS = {"dongdong/#", "data/#"};

    private static final String INFLUXDB_URL = "http://192.168.71.213:8086"; // InfluxDB URL
    private static final String TOKEN =
            "G8_uOapcNgd_0pxthosV4EDTwWnHsei46raAFy8poPwwGMZI79YU7LNdBruAdOC6WqVGiYTiSV-dWb_xauKm0A=="; // InfluxDB
                                                                                                        // 토큰
    private static final String ORGANIZATION = "dong's company"; // 조직 이름

    public static void main(String[] args) throws InterruptedException {

        try (MqttClient client = new MqttClient(BROKER, CLIENT_ID)) {

            // JSON 파싱을 위한 ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Connection lost: " + cause.getMessage());
                    // 자동 재연결 시도
                    while (!client.isConnected()) {
                        try {
                            System.out.println("Reconnecting...");
                            client.connect(options);
                            System.out.println("Reconnected!");
                            client.subscribe(TOPICS);
                        } catch (MqttException e) {
                            System.out.println("Reconnection failed: " + e.getMessage());
                            try {
                                Thread.sleep(5000); // 5초 후 재시도
                            } catch (InterruptedException ie) {
                                ie.printStackTrace();
                            }
                        }
                    }
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    if (topic.contains("lora") || topic.contains("power_meter")) {
                        return;
                    }

                    System.out.println("Received message from topic '" + topic + "': "
                            + new String(message.getPayload()));

                    try {
                        // MQTT 메시지 페이로드를 JSON으로 파싱
                        String payload = new String(message.getPayload());
                        JsonNode jsonNode = objectMapper.readTree(payload);

                        // time과 value 값을 추출
                        long timeMillis = jsonNode.get("time").asLong();
                        double value = jsonNode.get("value").asDouble();

                        // 토픽을 '/'로 분리하여 map으로 변환 태그로 사용
                        Map<String, String> topicMap = topicParser(topic);
                        String bucketToUse = "";
                        String pointMeasure = "";

                        // InfluxDB에 저장할 데이터 포인트 생성
                        if (topic.startsWith("dongdong/")) {
                            bucketToUse = "nhnacademy_data_2";
                            pointMeasure = "sensor_data";
                        } else if (topic.startsWith("data/")) {
                            bucketToUse = "nhnacademy_data_1";
                            pointMeasure = "room_data";
                        }

                        Point point = Point.measurement(pointMeasure);

                        // 동적으로 태그 추가
                        for (Map.Entry<String, String> entry : topicMap.entrySet()) {
                            point.addTag(entry.getKey(), entry.getValue());
                        }
                        point.addField("value", value).time(Instant.ofEpochMilli(timeMillis),
                                WritePrecision.MS); // 밀리초
                                                    // 단위
                                                    // 타임스탬프

                        // InfluxDB 클라이언트 생성
                        try (InfluxDBClient influxDBClient = InfluxDBClientFactory.create(
                                INFLUXDB_URL, TOKEN.toCharArray(), ORGANIZATION, bucketToUse);) {

                            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();

                            // InfluxDB에 데이터 저장
                            writeApi.writePoint(point);

                            System.out.println("Data written to InfluxDB: " + point);
                        }

                    } catch (Exception e) {
                        System.out.println("Error processing message: " + e.getMessage());
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("Message delivery complete: " + token.getMessageId());
                }
            });

            System.out.println("Connecting to broker...");
            client.connect(options);
            System.out.println("Connected!");

            System.out.println("Subscribing to topic: " + String.join(",", TOPICS));
            client.subscribe(TOPICS);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, String> topicParser(String topic) {
        String[] topicParts = topic.split("/");
        int numOfTag = topicParts.length / 2;
        Map<String, String> tags = new HashMap<>();

        for (int i = 0; i < numOfTag; i++) {
            String key = "unknown";

            switch (topicParts[2 * i + 1]) {
                case "s":
                    key = "site";
                    break;
                case "b":
                    key = "branch";
                    break;
                case "p":
                    key = "place";
                    break;
                case "e":
                    key = "element";
                    break;
                case "d":
                    key = "d";
                    break;
                case "sp":
                    key = "spot";
                    break;
                case "n":
                    key = "name";
                    break;
                case "g":
                    key = "g";
                    break;
                default:
                    break;
            }
            String value = topicParts[2 * i + 2];

            tags.put(key, value);
        }

        return tags;
    }
}
