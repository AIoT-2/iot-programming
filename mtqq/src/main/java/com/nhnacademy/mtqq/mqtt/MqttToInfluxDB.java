package com.nhnacademy.mtqq.mqtt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.write.Point;

import com.nhnacademy.mtqq.data.MessageData;
import com.nhnacademy.mtqq.exception.ExtractMessageException;
import com.nhnacademy.mtqq.exception.MessageDataIsNullException;
import com.nhnacademy.mtqq.exception.MqttMessageNotFoundException;
import com.nhnacademy.mtqq.exception.ObjectMapperIsNull;
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

    String broker;
    String clientId;
    String topic;

    private static final List<MessageData> messageList = new ArrayList<>();
    private static final String URL ="http://192.168.71.220:8086";
    private static final String ORG = "influx";
    private static final String BUCKET = "testForJava";
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
        if(mqttMessage != null){
            try (MqttClient client = new MqttClient(broker, clientId)) {
                MqttConnectOptions options = new MqttConnectOptions();
                options.setCleanSession(true);

                client.setCallback(new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable throwable) {
                        log.info("Connection lost: {}", throwable.getMessage());
                        try{
                            log.info("Reconnecting...");
                            client.connect(options);
                            log.info("Reconnected!");
                            client.subscribe(TOPIC);
                        }catch (MqttException e){
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

                        //JSON 파싱 및 필요한 데이터 추출
                        JsonNode jsonNode = objectMapper.readTree(payload);
                        MessageData messageData = extractmessageData(topic, jsonNode);

                        // 수신된 메시지를 JSON으로 저장
                        saveMessageToJson(messageData, objectMapper);
                        writeInfluxDB(messageData);
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

                // MQTT 메시지 발행
                log.debug("Publishing message to topic '{}': {}", topic, mqttMessage);
                client.publish(topic, new MqttMessage(mqttMessage.getBytes()));

                // 주제 구독
                log.info("Subscribing to topic: {}", topic);
                client.subscribe(topic);

                // 메시지 발행
                String message = "Hello, MQTT from Java!";
                log.info("Publishing message: {}", message);
                client.publish(topic, new MqttMessage(message.getBytes()));

                Thread.sleep(100000);

                log.info("Disconnecting...");
                client.disconnect();
                log.info("Disconnected!");

            } catch (MqttException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new MqttMessageNotFoundException("mqttMessage를 찾을 수 없습니다.");
        }
    }

    private static MessageData extractmessageData(String topic, JsonNode jsonNode){
        if(topic != null && jsonNode != null){
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
        }else{
            throw new ExtractMessageException("topic과 jsonNode가 null값입니다.");
        }
    }


    private static void saveMessageToJson(MessageData data, ObjectMapper objectMapper) {
        if(data != null && objectMapper != null){
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
        } else if(data == null){
            throw new MessageDataIsNullException("MessageData값이 null입니다.");
        } else {
            throw new ObjectMapperIsNull("objectMapper값이 null입니다.");
        }
    }

    private static void writeInfluxDB(MessageData data) {
        if(data != null){
            try (InfluxDBClient influxDBClient = InfluxDBClientFactory.create(URL, token, ORG, BUCKET)) {
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
                log.info("Data written to InfluxDB");
            } catch (Exception e) {
                log.debug("Error writing to InfluxDB: {}", e.getMessage());
            }
        } else {
            throw new MessageDataIsNullException("messageData값이 null입니다.");
        }
    }
}
