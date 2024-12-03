package com.nhnacademy;

import java.time.Instant;
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
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;

public class InfluxDBTest {
    private static final String BROKER = "tcp://192.168.70.203:1883";
    private static final String CLIENT_ID = "JavaClientExample";
    private static final String TOPIC = "application/#";

    private static final String INFLUX_URL = "http://192.168.71.222:8086";
    private static final String INFLUX_TOKEN = "jme1HOukU13gjU3ZHT6vCB7My1_MWD7q5zWhIDfWB6kf7owk_ms4fwKdyN_vo8RrPf6Rl09RYuv53i2RU7V70g==";
    private static final String INFLUX_ORG = "org";
    private static final String INFLUX_BUCKET = "test";

    public static void main(String[] args) throws InterruptedException{
        ObjectMapper mapper = new ObjectMapper();

        try (MqttClient client = new MqttClient(BROKER, CLIENT_ID)) {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    // System.out.println("Connection lost: " + cause.getMessage());
                    try {
                        client.connect(options);
                        client.subscribe(TOPIC);
                        System.out.println("Reconnected to broker.");
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    JsonNode rootNode = mapper.readTree(message.getPayload());
                    JsonNode objectNode = rootNode.path("object");
                    JsonNode deviceInfo = rootNode.path("deviceInfo");
                    String[] deviceSplit = deviceInfo.get("deviceName").asText().split("\\(");
                    String place = deviceSplit[1].replace(")", "");
                    InfluxDBClient influxDBClient = InfluxDBClientFactory.create(INFLUX_URL, INFLUX_TOKEN.toCharArray(), INFLUX_ORG, INFLUX_BUCKET);

                    Map<String, Object> dataMap = mapper.readValue(objectNode.toString(), Map.class);
                    for(Map.Entry<String, Object> entry : dataMap.entrySet()){
                        System.out.println(entry.getKey() + ": " + entry.getValue());
                    }
                    for(Map.Entry<String, Object> entry : dataMap.entrySet()){
                        String key = entry.getKey();
                        Object value = entry.getValue();

                        Point point = Point.measurement(key)
                                            .addTag("deviceName", deviceSplit[0])
                                            .addTag("location", place)
                                            .time(Instant.now().toEpochMilli(), WritePrecision.MS);

                        if(value instanceof Double){
                            point.addField("value", (Double) value);
                        } else if ( value instanceof Integer){
                            point.addField("value", (Integer) value);
                        } else if ( value instanceof Boolean){
                            point.addField("value", (Boolean) value);
                        } else if ( value instanceof String){
                            point.addField("value", (String) value);
                        }

                        influxDBClient.getWriteApiBlocking().writePoint(point);
                    }
                    
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("Message deliver complete: "+ token.getMessageId());
                }
            });

            System.out.println("Connecting to broker...");
            client.connect(options);
            System.out.println("Connected!");

            // 주제 구독
            System.out.println("Subscribing to topic: " + TOPIC);
            client.subscribe(TOPIC);

            Thread.sleep(100000);

            // 클라이언트 종료
            System.out.println("Disconnecting...");
            client.disconnect();
            System.out.println("Disconnected!");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
