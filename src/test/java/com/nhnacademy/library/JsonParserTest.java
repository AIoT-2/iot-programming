package com.nhnacademy.library;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.util.Property;
import com.nhnacademy.util.PropertyKey;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.StreamSupport;

@Slf4j
class JsonParserTest {

    private static JsonNode mainNode;

    private static JsonNode networkConfigNode;

    private static JsonNode mqttConfigNode;

    private final String EXPECT_IP = "tcp://192.168.71.219";

    private final String EXPECT_BROKER = "tcp://192.168.71.219:1883";

    private final String EXPECT_CLIENT_ID = "ATGN02-019";

    private final String EXPECT_TOPIC = "application/#";

    private static final String[] serviceNameList = {"mqtt", "node-red", "influxdb"};

    private static final Random random = new Random();

    @BeforeAll
    static void setUp() {

        try (InputStream inputStream = Property.class.getResourceAsStream(PropertyKey.CONFIG_PATH.getKey())
        ) {
            JsonNode mainNode = new ObjectMapper().readTree(inputStream);
            if (Objects.isNull(mainNode) || mainNode.isEmpty()) {
                log.warn("잘못된 데이터 경로 접근");
                throw new RuntimeException();
            }

            networkConfigNode = mainNode.path(PropertyKey.NETWORK_CONFIG.getKey());
            if (Objects.isNull(networkConfigNode) || networkConfigNode.isEmpty()) {
                log.warn("잘못된 Network Config 접근");
                throw new RuntimeException();
            }

            mqttConfigNode = mainNode.path(PropertyKey.MQTT_CONFIG.getKey());
            if (Objects.isNull(mqttConfigNode) || mqttConfigNode.isEmpty()) {
                log.warn("잘못된 MQTT Config 접근");
                throw new RuntimeException();
            }
        } catch (IOException e) {
            log.error("{}", e.getMessage(), e);
        }
    }

    @Order(1)
    @Test()
    @DisplayName("IP 주소 체크")
    void ipCheck() {
        String ip = networkConfigNode.get(PropertyKey.IP_ADDRESS.getKey()).asText();
        log.debug("IP: {}", ip);
        Assertions.assertEquals(EXPECT_IP, ip);
    }

    @Order(2)
    @Test
    @DisplayName("Port List 체크 1 (forEach)")
    void portListCheck1() {
        JsonNode portNodeList = networkConfigNode.path(PropertyKey.PORT_LIST.getKey());

        for (JsonNode node : portNodeList) {
            String serviceName = node.get(PropertyKey.SERVICE_NAME.getKey()).asText();
            String portNumber = node.get(PropertyKey.PORT_NUMBER.getKey()).asText();

            log.debug("{}", String.format("%-10s: %s", serviceName, portNumber));
        }
    }

    @Order(3)
    @Test
    @DisplayName("Port List 체크 2 (Stream)")
    void portListCheck2() {
        JsonNode portNodeList = networkConfigNode.path(PropertyKey.PORT_LIST.getKey());

        StreamSupport.stream(portNodeList.spliterator(), false)
                        .forEach(node -> {
                            String serviceName = node.path(PropertyKey.SERVICE_NAME.getKey()).asText();
                            String portNumber = node.path(PropertyKey.PORT_NUMBER.getKey()).asText();
                            log.debug("{}", String.format("%-10s: %s", serviceName, portNumber));
                            // `%-15s` or `%15s` 이것을 '문자열 정렬' 및 '고정 너비 출력', 'padding' 이라 불린다.
                        });
    }

    @Order(4)
    @Test
    @DisplayName("Json List 데이터를 Map 자료 구조에 매핑 1 (forEach)")
    void mappingTest1() {
        JsonNode portNodeList = networkConfigNode.path(PropertyKey.PORT_LIST.getKey());
        Map<String, String> portMap = new HashMap<>();

        for (JsonNode node : portNodeList) {
            String serviceName = node.get(PropertyKey.SERVICE_NAME.getKey()).asText();
            String portNumber = node.get(PropertyKey.PORT_NUMBER.getKey()).asText();

            portMap.put(serviceName, portNumber);
        }
    }

    @Order(5)
    @Test
    @DisplayName("Json List 데이터를 Map 자료 구조에 매핑 2 (Stream)")
    void mappingTest2() {
        JsonNode portNodeList = networkConfigNode.path(PropertyKey.PORT_LIST.getKey());
        Map<String, String> portMap = new HashMap<>();

        portNodeList.elements()
                    .forEachRemaining(node -> {
                        String serviceName = node.path("SERVICE").asText();
                        String portNumber = node.path("NUMBER").asText();
                        portMap.put(serviceName, portNumber);
                    }
        );

        portMap.forEach((k, v) -> log.debug("{}: {}", k, v));
    }

    @Order(6)
    @Test
    @DisplayName("클라이언트 ID 체크")
    void clientIdCheck() {
        // CLIENT_ID
        String clientId = mqttConfigNode.get("CLIENT_ID").asText();

        log.debug("ClientId: {}", clientId);
        Assertions.assertEquals(EXPECT_CLIENT_ID, clientId);
    }

    @Order(7)
    @Test
    @DisplayName("Topic 체크")
    void topicCheck() {
        // TOPIC
        String topic = mqttConfigNode.get("TOPIC").asText();

        log.debug("Topic: {}", topic);
        Assertions.assertEquals(EXPECT_TOPIC, topic);
    }

    @Order(8)
    @Test
    @DisplayName("번외 - JsonNode 내부에 특정 서비스의 Endpoint 호출")
    void brokerCheck1() {
        JsonNode portNodeList = networkConfigNode.path(PropertyKey.PORT_LIST.getKey());
        String findServiceName = serviceNameList[getRandomIndex()];

        JsonNode mqttPort = StreamSupport.stream(portNodeList.spliterator(), false)
                                            .filter(node ->
                                                    node.get(PropertyKey.SERVICE_NAME.getKey())
                                                        .asText()
                                                        .equals(findServiceName))
                                            .findFirst()
                                            .orElseThrow(() -> {
                                                log.warn("{} 서비스의 Port 데이터를 찾을 수 없습니다.", findServiceName);
                                                return new NoSuchElementException();
                                            });
        String ip = networkConfigNode.get(PropertyKey.IP_ADDRESS.getKey()).asText();
        String port = mqttPort.get(PropertyKey.PORT_NUMBER.getKey()).asText();

        String broker = ip + ":" + port;
        log.debug("Broker: {}", broker);
    }

    // 번외 - 알아보기 매우 난해하므로 추천하지 않는다.
    @Order(9)
    @Test
    @DisplayName("번외 - JsonNode 내부에 특정 서비스의 Endpoint 호출")
    void brokerCheck2() {
        // MQTT BROKER
        AtomicReference<String> ipRef = new AtomicReference<>();
        AtomicReference<String> portRef = new AtomicReference<>();

        JsonNode portNodeList = networkConfigNode.path(PropertyKey.PORT_LIST.getKey());
        String findServiceName = serviceNameList[getRandomIndex()];

        StreamSupport.stream(portNodeList.spliterator(), false)
                        .filter(node ->
                                node.get(PropertyKey.SERVICE_NAME.getKey())
                                    .asText()
                                    .equals(findServiceName))
                        .findFirst()
                        .ifPresentOrElse(
                                mqttPort -> {
                                    ipRef.set(networkConfigNode.get(PropertyKey.IP_ADDRESS.getKey()).asText());
                                    portRef.set(mqttPort.get(PropertyKey.PORT_NUMBER.getKey()).asText());
                                },
                                () -> {
                                    log.warn("{} 서비스의 Port 데이터를 찾을 수 없습니다.", findServiceName);
                                    throw new NoSuchElementException();
                                }
                        );
        String ip = ipRef.get();
        String port = portRef.get();

        String broker = ip + ":" + port;
        log.debug("Broker: {}", broker);
    }

    public static int getRandomIndex() {
        return random.nextInt(serviceNameList.length);
    }
}
