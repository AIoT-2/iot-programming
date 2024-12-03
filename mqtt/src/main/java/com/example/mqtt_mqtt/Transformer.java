package com.example.mqtt_mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.mqtt_mqtt.SensorData;

public class Transformer {

    // Jackson ObjectMapper 인스턴스 생성
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * JSON 메시지를 가공하는 메서드
     * @param topic MQTT 메시지의 topic
     * @param jsonMessage MQTT 메시지 (JSON 형식)
     * @return 변환된 데이터 (예: 가공된 JSON 문자열)
     */
    public String transform(String topic, String jsonMessage) {
        try {
            // JSON 메시지를 Java 객체로 변환
            SensorData data = objectMapper.readValue(jsonMessage, SensorData.class);

            // topic과 마지막 단어 처리
            String[] topicParts = topic.split("/");
            String lastWord = topicParts[topicParts.length - 1];
            String newTopic = "sensor/" + lastWord;
            data.setTopic(newTopic);
            data.setValuename(lastWord);

            // value 변환 (예: 두 배로 증가)
            data.setValue(data.getValue());

            // 다시 JSON 문자열로 변환 후 반환
            return objectMapper.writeValueAsString(data);

        } catch (Exception e) {
            e.printStackTrace();
            // 변환 실패 시 원본 메시지를 반환하거나 null 처리
            return null;
        }
    }
}
