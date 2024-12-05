package com.example.mqtt_mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


public class MainSingle {
    // MQTT 브로커 주소
    private static final String BROKER = "tcp://192.168.70.203:1883";
    // 클라이언트 ID
    private static final String CLIENT_ID = "JavaClientExample";
    // 구독 및 발행 주제
    private static final String TOPIC = "data/#";
    // Jackson ObjectMapper
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) throws InterruptedException {
        try (MqttClient client = new MqttClient(BROKER, CLIENT_ID)) {

            // 연결 설정
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true); // 클린 세션 사용

            // 메시지 수신 콜백 설정
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Connection lost: "
                            + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message)
                        throws Exception {
                    String payload = new String(message.getPayload());
                    System.out.println("받은 메세지의 토픽 '" + topic + "': " + payload);
                    String[] ex1 = topic.split("/");
                    

                    JsonNode rootNode = MAPPER.readTree(payload);
                    
                    String columns = "";
                    double value = 0.0;
                    long time = 0;
                    String tag = "";
                    String pub_topic = "";
                    
                    if(topic.contains("e")) {
                        columns = ex1[ex1.length - 1];
                        value = rootNode.path("value").asDouble();
                        time = rootNode.path("time").asLong();
                        tag = String.format("%s(%s)", ex1[8], ex1[ex1.toString().indexOf("n") + 1]);
                        pub_topic = "sensor/" + columns;
                    }

                    TransformedData transformedData = new TransformedData(columns, value, time, tag);

                    String transformedMapper = MAPPER.writeValueAsString(transformedData);

                    client.publish(pub_topic, new MqttMessage(transformedMapper.getBytes()));
                    System.out.println("Pub to topic: " + pub_topic);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("Message delivery complete: "
                            + token.getMessageId());
                }
            });

            // 브로커 연결
            System.out.println("Connecting to broker...");
            client.connect(options);
            System.out.println("Connected!");

            // 주제 구독
            System.out.println("Subscribing to topic: " + TOPIC);
            client.subscribe(TOPIC);

            // 메시지 발행
            String message = "Hello, MQTT from Java!";
            System.out.println("Publishing message: " + message);
            client.publish(TOPIC, new MqttMessage(message.getBytes()));

            // 10초 대기 후 종료
            Thread.sleep(100000);

            // 클라이언트 종료
            System.out.println("Disconnecting...");
            client.disconnect();
            System.out.println("Disconnected!");

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    static record TransformedData(String columns, double value, long time, String tag) {
        
    }
}
