package com.nhnacademy.library;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.util.Property;
import com.nhnacademy.util.PropertyKey;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.StreamSupport;

@Slf4j
class JsonParserTest {

    private static JsonNode node;

    private static JsonNode portNode;

    private static JsonNode mqttNode;

    private final String EXPECT_IP = "tcp://192.168.71.219";

    private final String EXPECT_BROKER = "tcp://192.168.71.219:1883";

    private final String EXPECT_CLIENT_ID = "ATGN02-019_";

    private final String EXPECT_TOPIC = "application/#";

    @BeforeAll
    static void setUp() {
        try (InputStream inputStream = Property.class.getResourceAsStream(PropertyKey.CONFIG_PATH.getKey())) {
            node = new ObjectMapper()
                        .readTree(inputStream)
                        .path(PropertyKey.CONFIG.getKey());
            if (node.isEmpty()) {
                log.warn("잘못된 Config 접근");
                throw new RuntimeException();
            }

            portNode = node.path(PropertyKey.PORT.getKey()); // list 구조
            if (!portNode.isArray() || portNode.isEmpty()) {
                log.warn("잘못된 Port 접근");
                throw new RuntimeException();
            }

            mqttNode = node.path(PropertyKey.MQTT.getKey());
            if (mqttNode.isEmpty()) {
                log.warn("잘못된 MQTT 접근");
                throw new RuntimeException();
            }
        } catch (IOException e) {
            log.error("{}", e.getMessage(), e);
        }
    }

    @Order(1)
    @Test()
    @DisplayName("")
    void ipCheck() {
        String ip = node.get(PropertyKey.IP_ADDRESS.getKey()).asText();
        log.debug("IP: {}", ip);
        Assertions.assertEquals(EXPECT_IP, ip);
    }

    @Order(2)
    @Test
    @DisplayName("")
    void portListCheck() {
        StreamSupport.stream(portNode.spliterator(), false)
                        .forEach(node -> {
                            String service = node.path(PropertyKey.SERVICE.getKey()).asText();
                            String number = node.path(PropertyKey.NUMBER.getKey()).asText();
                            log.debug("{}", String.format("%-10s: %s", service, number));
                            // `%-15s` or `%15s` 이것을 '문자열 정렬' 및 '고정 너비 출력', 'padding' 이라 불린다.
                        });
    }

    @Order(3)
    @Test
    @DisplayName("")
    void brokerCheck1() {
        JsonNode mqtt = StreamSupport.stream(portNode.spliterator(), false)
                                        .filter(node ->
                                            node.get(PropertyKey.SERVICE.getKey())
                                                .asText()
                                                .equals(PropertyKey.MQTT.getKey()))
                                        .findFirst()
                                        .orElseThrow(() -> {
                                            log.warn("MQTT 데이터를 찾을 수 없습니다.");
                                            return new NoSuchElementException();
                                        });
        String ip = mqtt.get(PropertyKey.IP_ADDRESS.getKey()).asText();
        String port = mqtt.get(PropertyKey.PORT.getKey()).asText();

        String broker = ip + ":" + port;
        log.debug("Broker: {}", broker);
        Assertions.assertEquals(EXPECT_BROKER, broker);
    }

    // 번외 - 알아보기 매우 난해하므로 추천하지 않는다.
    @Order(4)
    @Test
    @DisplayName("")
    void brokerCheck2() {
        // MQTT BROKER
        AtomicReference<String> ipRef = new AtomicReference<>();
        AtomicReference<String> portRef = new AtomicReference<>();

        StreamSupport.stream(portNode.spliterator(), false)
                        .filter(node ->
                            node.get(PropertyKey.SERVICE.getKey())
                                .asText()
                                .equals(PropertyKey.MQTT.getKey()))
                        .findFirst()
                        .ifPresentOrElse(
                            mqtt -> {
                                ipRef.set(mqtt.get(PropertyKey.IP_ADDRESS.getKey()).asText());
                                portRef.set(mqtt.get(PropertyKey.PORT.getKey()).asText());
                            },
                            () -> {
                                log.warn("MQTT 데이터를 찾을 수 없습니다.");
                                throw new NoSuchElementException();
                            }
                        );
        String ip = ipRef.get();
        String port = portRef.get();

        String broker = ip + ":" + port;
        log.debug("Broker: {}", broker);
        Assertions.assertEquals(EXPECT_BROKER, broker);
    }

    @Order(5)
    @Test
    @DisplayName("")
    void clientIdCheck() {
        // CLIENT_ID
        String clientId = mqttNode.get("CLIENT_ID").asText();

        log.debug("ClientId: {}", clientId);
        Assertions.assertEquals(EXPECT_CLIENT_ID, clientId);
    }

    @Order(6)
    @Test
    @DisplayName("")
    void topicCheck() {
        // TOPIC
        String topic = mqttNode.get("TOPIC").asText();

        log.debug("Topic: {}", topic);
        Assertions.assertEquals(EXPECT_TOPIC, topic);
    }
}
