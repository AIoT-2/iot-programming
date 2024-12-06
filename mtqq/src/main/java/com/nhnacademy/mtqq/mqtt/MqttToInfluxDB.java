package com.nhnacademy.mtqq.mqtt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.write.Point;

import com.nhnacademy.mtqq.exception.MqttMessageNotFoundException;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MqttToInfluxDB {
    private static final String BROKER;
    private static final String CLIENT_ID;
    private static final String TOPIC = "data/";
    private static final Logger log = LoggerFactory.getLogger(MqttToInfluxDB.class);

    String broker;
    String clientId;
    String topic;
    String url;
    String org;
    String bucket;

    private static final List<Map<String, Object>> messageList = new ArrayList<Map<String, Object>>();
    private static final String URL;
    private static final String ORG;
    private static final String BUCKET;
    private static final char[] token;

    static {
        Properties properties = new Properties();
        try(InputStream input = MqttToInfluxDB.class.getClassLoader().getResourceAsStream("application.properties")){
            if(input == null){
                throw new RuntimeException("설정 파일(application.properties)을 찾을 수 없습니다.");
            }
            properties.load(input);
            String brokerPath = properties.getProperty("broker.path");
            String brokerPort = properties.getProperty("broker.port");
            String c = properties.getProperty("client.id");

            if(brokerPath == null || brokerPort == null || c == null){
                throw new IllegalStateException("brokerPath, brokerPort, clientId, topic 기본값이 설정 파일에 없습니다.");
            }
            BROKER = brokerPath +":"+brokerPort;
            CLIENT_ID = c;

            log.debug("url: {}, org: {}, bucket: {}", BROKER, CLIENT_ID, TOPIC);

            String dbPath = properties.getProperty("DB.path");
            String dbPort = properties.getProperty("DB.port");
            String o = properties.getProperty("org");
            String b = properties.getProperty("bucket");
            token = properties.getProperty("INFLUXDB_TOKEN").toCharArray();
            if(dbPath == null || dbPort == null || o == null || b == null){
                throw new IllegalStateException("path 또는 port, org, bucket 값이 설정 파일에 없습니다.");
            }
            URL = dbPath + ":" + dbPort;
            ORG = o;
            BUCKET = b;

            log.debug("url: {}, org: {}, bucket: {}", URL, ORG, BUCKET);
        } catch (IOException e) {
            throw new RuntimeException("설정 파일 로드 중 오류 발생", e);
        }
    }

    public MqttToInfluxDB(String broker, String clientId, String topic, String url, String org, String bucket){
        if(broker != null && clientId != null && topic != null && url != null && org != null && bucket != null){
            this.broker = broker;
            this.clientId = clientId;
            this.topic = topic;
            this.url = url;
            this.org = org;
            this.bucket = bucket;
        } else {
            throw new IllegalArgumentException("broker, clientId, topic이 null입니다.");
        }
    }

    public MqttToInfluxDB(){
        this.broker = BROKER;
        this.clientId = CLIENT_ID;
        this.topic = TOPIC;
        this.url = URL;
        this.org = ORG;
        this.bucket = BUCKET;
    }

    public void run(Map<String, Object> mqttMessageData) throws InterruptedException {
            try (MqttClient client = new MqttClient(broker, clientId)) {
                MqttConnectOptions options = new MqttConnectOptions();
                options.setCleanSession(true);

                client.setCallback(new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable throwable) {
                        log.info("Connection lost: {}", throwable.getMessage());
                        try {
                            log.info("Reconnecting...");
                            client.connect(options);
                            log.info("Reconnected!");
                            client.subscribe(TOPIC);
                        } catch (MqttException e) {
                            log.debug(e.getMessage());
                        }
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                        String payload = new String(mqttMessage.getPayload());
                        log.info("Received message from topic '{}': {}", topic, payload);

                        // JSON 처리용 ObjectMapper 생성
                        ObjectMapper objectMapper = new ObjectMapper();
                        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

                        // JSON 파싱 및 데이터 추출
                        JsonNode jsonNode = objectMapper.readTree(payload);
                        Map<String, Object> messageData = extractMessageData(topic, jsonNode);

                        // 메시지를 JSON으로 저장
                        saveMessageToJson(messageData, objectMapper);
                        writeInfluxDB(messageData, url, org, bucket);
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {
                        log.info("Message delivery complete: {}", token.getMessageId());
                    }
                });

                // 브로커 연결
                log.info("Connection to broker...");
                client.connect(options);
                log.info("Connected!");

                // Map 데이터를 JSON으로 변환하여 MQTT 메시지 발행
                ObjectMapper objectMapper = new ObjectMapper();
                String messagePayload = objectMapper.writeValueAsString(mqttMessageData);  // Map을 JSON String으로 변환
                log.debug("Publishing message to topic '{}': {}", topic, messagePayload);
                client.publish(topic, new MqttMessage(messagePayload.getBytes()));

                // 주제 구독
                log.info("Subscribing to topic: {}", topic);
                client.subscribe(topic);

                Thread.sleep(100000);

                log.info("Disconnecting...");
                client.disconnect();
                log.info("Disconnected!");

            } catch (MqttException e) {
                throw new RuntimeException(e);
            } catch (JsonProcessingException e) {
                log.error("Error processing JSON message", e);
            }
    }


    private static Map<String, Object> extractMessageData(String topic, JsonNode jsonNode) {
        if (topic != null && jsonNode != null) {
            Map<String, Object> messageData = new HashMap<>();
            // 토픽에서 deviceName 추출
            String[] topicParts = topic.split("/");
            for (int i = 0; i < topicParts.length; i++) {
                if ("n".equals(topicParts[i]) && i + 1 < topicParts.length) {
                    messageData.put("deviceName", topicParts[i + 1]);
                    break;
                }
            }

            // payload에서 필요한 데이터 추출
            messageData.put("topic", topic);
            messageData.put("message", jsonNode.toString());
            messageData.put("voltage", jsonNode.path("voltage").asDouble(0.0));
            messageData.put("current", jsonNode.path("current").asDouble(0.0));
            messageData.put("activePower", jsonNode.path("activePower").asDouble(0.0));
            messageData.put("reactivePower", jsonNode.path("reactivePower").asDouble(0.0));
            messageData.put("tapparentPower", jsonNode.path("tapparentPower").asDouble(0.0));
            messageData.put("phase", jsonNode.path("phase").asInt(0));
            messageData.put("powerFactor", jsonNode.path("powerFactor").asDouble(0.0));

            return messageData;
        } else {
            throw new IllegalArgumentException("topic과 jsonNode가 null입니다.");
        }
    }


    private static void saveMessageToJson(Map<String, Object> data, ObjectMapper objectMapper) {
        if (data != null && objectMapper != null) {
            try {
                // 메시지 리스트에 추가
                messageList.add(data);

                // JSON 파일로 저장
                File jsonFile = new File("/Desktop/Json/message.json");
                objectMapper.writeValue(jsonFile, messageList);
                log.info("Message saved as JSON: {}", jsonFile.getAbsolutePath());

            } catch (IOException e) {
                log.debug("Error saving message to JSON: {}", e.getMessage());
            }
        } else {
            log.error("MessageData or ObjectMapper is null");
        }
    }

    private static void writeInfluxDB(Map<String, Object> data, String url, String org, String bucket) {
        if (data != null && url != null && org != null && bucket != null) {
            try (InfluxDBClient influxDBClient = InfluxDBClientFactory.create(url, token, org, bucket)) {
                WriteApiBlocking writeApiBlocking = influxDBClient.getWriteApiBlocking();

                Point point = Point.measurement("energy_data")
                        .addTag("deviceName", (String) data.get("deviceName"))
                        .addFields(data);

                writeApiBlocking.writePoint(point);
                log.info("Data written to InfluxDB");
            } catch (Exception e) {
                log.debug("Error writing to InfluxDB: {}", e.getMessage());
            }
        } else {
            log.error("Data is null");
        }
    }
}
