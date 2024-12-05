package com.nhnacademy.mqtt;

import java.sql.Timestamp;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {
    public static void main(String[] args) {
        ObjectMapper mapper = new ObjectMapper();
        MqttSubscriber subscribe;
        MqttPublisher publish = new MqttPublisher("tcp://192.168.71.222:1883", "JavaClientExample");

        MessageHandler messageHandler = (topic, message) -> {
            try {
                JsonNode rootNode;
                rootNode = mapper.readTree(message);
                JsonNode objectNode = rootNode.path("object");
                if(!objectNode.isMissingNode()){
                    String devEui = rootNode.path("deviceInfo").get("devEui").asText();
                    Map<String, Object> objectMap = mapper.readValue(objectNode.toString(), Map.class);
                    Map<String, Object> tagMap = mapper.readValue(rootNode.path("deviceInfo").get("tags").toString(), Map.class);
                    StringBuilder myTopic = new StringBuilder("data/s/").append(tagMap.get("site"))
                                                    .append("/b/").append(tagMap.get("branch"))
                                                    .append("/p/").append(tagMap.get("place"))
                                                    .append("/d/").append(devEui);          
    
                    if(tagMap.containsKey("spot")){
                        myTopic.append("/sp/").append(tagMap.get("spot").toString());
                    }
                    if (tagMap.containsKey("name")) {
                        myTopic.append("/n/").append(tagMap.get("name").toString().replace(" ", ""));
                    }
    
                
                    for (Map.Entry<String, Object> entry : objectMap.entrySet()) {
                        String key = entry.getKey();
                        Object value = entry.getValue();
                    
                        String topicWithKey = myTopic.toString() + "/e/" + key;
                    
                        Map<String, Object> tagMessage = Map.of(
                            "time", new Timestamp(System.currentTimeMillis()),
                            "value", value
                        );
                    
                        String newMessage = mapper.writeValueAsString(tagMessage);
                        publish.send(topicWithKey, newMessage, 1);
                    }
                }
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        };
        
        subscribe = new MqttSubscriber("tcp://192.168.70.203:1883", "JavaClientExample", messageHandler);
        subscribe.getMessage("application/#");

        try {
            Thread.sleep(100000);
            
            subscribe.close();
            publish.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
