package com.nhnacademy;

import java.sql.Timestamp;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MqttPublish {
    private static final String BROKER = "tcp://192.168.70.203:1883";
    private static final String CLIENT_ID = "JavaClientExample";
    private static final String TOPIC = "application/#";

    private static final String OTHER_BROKER = "tcp://192.168.71.222:1883";
    private static final String OTHER_CLIENT_ID = "JavaClientExample";

    public static void main(String[] args) throws InterruptedException{
        ObjectMapper mapper = new ObjectMapper();
        try (
            MqttClient client = new MqttClient(BROKER, CLIENT_ID); 
            MqttClient myClient = new MqttClient(OTHER_BROKER, OTHER_CLIENT_ID);
        ) {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Connection lost: " + cause.getMessage());
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
                    
                    if(!objectNode.isMissingNode()){
                        String devEui = rootNode.path("deviceInfo").get("devEui").asText();
                        Map<String, Object> objectMap = mapper.readValue(objectNode.toString(), Map.class);
                        Map<String, Object> tagMap = mapper.readValue(rootNode.path("deviceInfo").get("tags").toString(), Map.class);
                        StringBuilder myTopic = new StringBuilder("data/s/").append(tagMap.get("site"))
                                                        .append("/b/").append(tagMap.get("branch"))
                                                        .append("/p/").append(tagMap.get("place"))
                                                        .append("/d/").append(devEui);

                        if (tagMap.containsKey("spot")) {
                            myTopic.append("/sp/").append(tagMap.get("spot"));
                        }

                        if (tagMap.containsKey("name")) {
                            myTopic.append("/n/").append(tagMap.get("name").toString().replace(" ", ""));
                        }
                        
    
                        for (Map.Entry<String, Object> entry : objectMap.entrySet()) {
                            String key = entry.getKey();
                            Object value = entry.getValue();
 
                            String topicWithKey = myTopic.append("/e/").append(key).toString();
                
                            Map<String, Object> tagMessage = Map.of(
                                "time", new Timestamp(System.currentTimeMillis()),
                                "value", value
                            );
                
                            String newmessage = mapper.writeValueAsString(tagMessage);
                            MqttMessage mqttMessage = new MqttMessage(newmessage.getBytes());
                            mqttMessage.setQos(1);
                
                            myClient.publish(topicWithKey, mqttMessage);

                            System.out.println("Publishing to topic: " + topicWithKey);
                            System.out.println("Message: " + tagMessage);
                        }
                    }

                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("Message deliver complete: "+ token.getMessageId());
                }

            });
            
            connectToBroker(client, options, BROKER);
            connectToBroker(myClient, options, OTHER_BROKER);
 
 
            System.out.println("Subscribing to topic: " + TOPIC);
            client.subscribe(TOPIC);
 
            Thread.sleep(100000);
 
            System.out.println("Disconnecting...");
            client.disconnect();
            myClient.disconnect();
            System.out.println("Disconnected!");
       } catch (MqttException e) {
           e.printStackTrace();
       }
   }

   // 브로커 연결 메서드
   private static void connectToBroker(MqttClient client, MqttConnectOptions options, String broker) throws MqttException {
       System.out.println("Connecting to broker: " + broker);
       client.connect(options);
       System.out.println("Connected to " + broker);
   }
}