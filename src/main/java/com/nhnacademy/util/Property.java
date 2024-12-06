package com.nhnacademy.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * MQTT Client 속성 값 <br>
 * {@code /resources/config.json} 참조
 */
@Slf4j
public final class Property {

    private static final String TIME_FORMAT;

    /**
     * IP 주소
     */
    private static final String IP_ADDRESS;

    /**
     * PORT 번호 목록
     */
    private static final Map<String, String> PORT_MAP = new HashMap<>();

    /**
     * 클라이언트 ID
     */
    private static final String CLIENT_ID;

    /**
     * 구독 및 발행 주제
     */
    private static final String TOPIC;

    // =================================================================================================================
    // Initial Settings

    static {
        try (InputStream inputStream = Property.class.getResourceAsStream(PropertyKey.CONFIG_PATH.getKey())
        ) {
            if (Objects.isNull(inputStream)) {
                log.warn("resources file 'config.json' is not exist");
                throw new RuntimeException();
            }
            JsonNode main = new ObjectMapper().readTree(inputStream);
            nodeCheck(main);

            JsonNode networkConfigNode = main.path(PropertyKey.NETWORK_CONFIG.getKey());
            JsonNode mqttConfigNode = main.path(PropertyKey.MQTT_CONFIG.getKey());

            createPortMap(networkConfigNode.path(PropertyKey.PORT_LIST.getKey()));

            TIME_FORMAT = main
                            .get(PropertyKey.TIME_FORMAT.getKey())
                            .asText();
            IP_ADDRESS = networkConfigNode
                            .get(PropertyKey.IP_ADDRESS.getKey())
                            .asText();
            CLIENT_ID = mqttConfigNode
                            .get(PropertyKey.CLIENT_ID.getKey())
                            .asText();
            TOPIC = mqttConfigNode
                            .get(PropertyKey.TOPIC.getKey())
                            .asText();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void createPortMap(JsonNode portNodeList) {
        portNodeList.elements()
                    .forEachRemaining(portNode -> {
                            String serviceName = portNode
                                                    .get(PropertyKey.SERVICE_NAME.getKey())
                                                    .asText();
                            String portNumber = portNode
                                                    .get(PropertyKey.PORT_NUMBER.getKey())
                                                    .asText();
                            PORT_MAP.put(serviceName, portNumber);
                        }
                    );
    }

    // =================================================================================================================
    // Getter

    public static String getTimeFormat() {
        return TIME_FORMAT;
    }

    public static String getIpAddress() {
        return IP_ADDRESS;
    }

    /**
     * <h5>Info</h5>
     * 특정 서비스의 Port 번호를 반환합니다.
     * <hr>
     *
     * @param serviceName 서비스 이름
     * @return Port 번호
     * @throws RuntimeException {@code /resources/config.json}에 존재하지 않는 서비스 데이터를 호출한 경우
     */
    public static String getServicePort(String serviceName) {
        if (!PORT_MAP.containsKey(serviceName)) {
            log.warn("Check your port data");
            nodeIsNotExist(serviceName);
        }
        return PORT_MAP.get(serviceName);
    }

    /**
     * <h5>Endpoint란?</h5>
     * 네트워크에서 연결 가능한 서비스의 접속 지점을 의미합니다. <br>
     * <b>Ex) 192.168.70.203:8080</b>
     * <hr>
     *
     * <h5>Info</h5>
     * 특정 서비스의 Endpoint를 반환합니다.
     * <hr>
     *
     * @param serviceName 서비스 이름
     * @return Endpoint
     */
    public static String getEndpoint(String serviceName) {
        return getIpAddress() + ":" + getServicePort(serviceName);
    }

    /**
     * @return 클라이언트 ID
     */
    public static String getClientId() {
        return CLIENT_ID;
    }

    /**
     * @return 구독 및 발행 주제
     */
    public static String getTopic() {
        return TOPIC;
    }

    // =================================================================================================================
    // Check Logic || Exception

    private static void nodeCheck(JsonNode main) {
        if (main.isMissingNode())
            nodeIsNotExist("All");

        if (main.path(PropertyKey.NETWORK_CONFIG.getKey()).isMissingNode())
            nodeIsNotExist(PropertyKey.NETWORK_CONFIG.getKey());

        if (main.path(PropertyKey.MQTT_CONFIG.getKey()).isMissingNode())
            nodeIsNotExist(PropertyKey.MQTT_CONFIG.getKey());
    }

    private static void nodeIsNotExist(String keyName) {
        log.warn("{} property value does not exist.", keyName);
        throw new NoSuchElementException();
    }
}
