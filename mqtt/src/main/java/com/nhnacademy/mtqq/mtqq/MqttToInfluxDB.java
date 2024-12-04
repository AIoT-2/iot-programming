package com.nhnacademy.mtqq.mtqq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.write.Point;

import com.nhnacademy.mtqq.data.MessageData;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MqttToInfluxDB {
    private static final String BROKER  = "tcp://192.168.70.203:1883";
    private static final String CLIENT_ID = "JavaClientExample";
    private static final String TOPIC = "data/#";
    private static final Logger log = LoggerFactory.getLogger(MqttToInfluxDB.class);

    private final String broker;
    private final String clientId;
    private final String topic;

    private static final List<MessageData> messageList = new ArrayList<>();
    private static final String url ="http://192.168.71.220:8086";
    private static final String org = "influx";
    private static final String bucket = "testForJava";
    private static final char[] token;

    static {
        String tokenEnv = System.getenv("INFLUXDB_TOKEN");
        log.debug("INFLUXDB_TOKEN : {}", System.getenv("INFLUXDB_TOKEN"));
        if(tokenEnv == null || tokenEnv.isEmpty()){
            throw new IllegalStateException("INFULXDB_TOKEN is not set. Using defulat token.");
        }
        token = tokenEnv.toCharArray();
    }

    public MqttToInfluxDB(String broker, String clientId, String topic){
        if(broker != null || clientId != null || topic != null){
            this.broker = broker;
            this.clientId = clientId;
            this.topic = topic;

        } else {
            throw new IllegalArgumentException("broker와 clientId, topic이 null입니다.");
        }
    }

    public MqttToInfluxDB(){
        this.broker = BROKER;
        this.clientId = CLIENT_ID;
        this.topic = TOPIC;
    }

    public void run(String mqttMessage) throws InterruptedException {
        try (MqttClient client = new MqttClient(broker, clientId)) {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable throwable) {
                    System.out.println("Connection lost: " + throwable.getMessage());
                    try{
                        System.out.println("Reconnecting...");
                        client.connect(options);
                        System.out.println("Reconnected!");
                        client.subscribe(TOPIC);
                    }catch (MqttException e){
                        log.info(e.getMessage());
                    }
                }

                @Override
                public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                    String payload = new String(mqttMessage.getPayload());
                    System.out.println("Received message from topic '" + topic + "': " + payload);

                    // JSON 처리용 ObjectMapper 생성
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

                    //JSON 파싱 및 필요한 데이터 추출
                    JsonNode jsonNode = objectMapper.readTree(payload);
                    MessageData messageData = extractmessageData(topic, jsonNode);

                    // 수신된 메시지를 JSON으로 저장
                    saveMessageToJson(messageData, objectMapper);
                    writeInfluxDB(messageData);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("Message delivery complete: " + token.getMessageId());
                }
            });

            // 브로커 연결
            System.out.println("Connection to broker...");
            client.connect(options);
            System.out.println("Connected!");

            // MQTT 메시지 발행
            log.debug("Publishing message to topic '{}': {}", topic, mqttMessage);
            client.publish(topic, new MqttMessage(mqttMessage.getBytes()));

            // 주제 구독
            System.out.println("Subscribing to topic: " + topic );
            client.subscribe(topic);

            // 메시지 발행
            String message = "Hello, MQTT from Java!";
            System.out.println("Publishing message: " + message);
            client.publish(topic, new MqttMessage(message.getBytes()));

            Thread.sleep(100000);

            System.out.println("Disconnecting...");
            client.disconnect();
            System.out.println("Disconnected!");

        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    private static MessageData extractmessageData(String topic, JsonNode jsonNode){
        MessageData messageData = new MessageData();
        // 토픽에서 deviceName 추출
        String[] topicParts = topic.split("/");
        for (int i = 0; i < topicParts.length; i++) {
            if ("n".equals(topicParts[i]) && i + 1 < topicParts.length) {
                messageData.setDeviceName(topicParts[i + 1]);
                break;
            }
        }
        // payload에서 필요한 데이터 추출
        messageData.setTopic(topic);
        messageData.setMessage(jsonNode.toString()); // 원본 메시지 저장
        messageData.setVoltage(jsonNode.path("voltage").asDouble(0.0));
        messageData.setCurrent(jsonNode.path("current").asDouble(0.0));
        messageData.setActivePower(jsonNode.path("activePower").asDouble(0.0));
        messageData.setReactivePower(jsonNode.path("reactivePower").asDouble(0.0));
        messageData.setTapparentPower(jsonNode.path("tapparentPower").asDouble(0.0));
        messageData.setPhase(jsonNode.path("phase").asInt(0));
        messageData.setPowerFactor(jsonNode.path("powerFactor").asDouble(0.0));

        return messageData;
    }


    private static void saveMessageToJson(MessageData data, ObjectMapper objectMapper) {
        try {
            // 메시지 리스트에 추가
            messageList.add(data);

            // JSON 파일로 저장
            File jsonFile = new File("message.json");
            objectMapper.writeValue(jsonFile, messageList);
            System.out.println("Message saved as JSON: " + jsonFile.getAbsolutePath());

        } catch (IOException e) {
            System.out.println("Error saving message to JSON: " + e.getMessage());
        }
    }

    private static void writeInfluxDB(MessageData data) {
        try (InfluxDBClient influxDBClient = InfluxDBClientFactory.create(url, token, org, bucket)) {
            WriteApiBlocking writeApiBlocking = influxDBClient.getWriteApiBlocking();

            Point point = Point.measurement("energy_data")
                    .addTag("deviceName", data.getDeviceName())
                    .addField("voltage", data.getVoltage())
                    .addField("current", data.getCurrent())
                    .addField("activePower", data.getActivePower())
                    .addField("reactivePower", data.getReactivePower())
                    .addField("tapparentPower", data.getTapparentPower())
                    .addField("phase", data.getPhase())
                    .addField("powerFactor", data.getPowerFactor());

            writeApiBlocking.writePoint(point);
            System.out.println("Data written to InfluxDB");
        } catch (Exception e) {
            System.out.println("Error writing to InfluxDB: " + e.getMessage());
        }
    }

}
