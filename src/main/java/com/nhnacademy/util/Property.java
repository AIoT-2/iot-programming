package com.nhnacademy.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * MQTT Client 속성 값 <br>
 * {@code /resources/config.json} 참조
 */
@Slf4j
public final class Property {

    /**
     * IP 주소
     */
    private static final String IP_ADDRESS;

    /**
     * PORT 번호 목록
     */
    private static final Map<String, String> PORT_MAP;

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
        try (InputStream inputStream = Property.class.getResourceAsStream(PropertyKey.CONFIG_PATH.getKey())) {
            if (Objects.isNull(inputStream)) {
                log.warn("resources file 'config' is not exist");
                throw new RuntimeException();
            }
            JsonNode config = new ObjectMapper().readTree(inputStream);
            propertyCheck(config);

            IP_ADDRESS = config.path(PropertyKey.IP_ADDRESS.getKey()).asText();

            PORT_MAP = StreamSupport.stream(config.path(PropertyKey.PORT.getKey()).spliterator(), false)
                                    .collect(Collectors.toMap(
                                        node -> node.path(PropertyKey.SERVICE.getKey()).asText(),
                                        node -> node.path(PropertyKey.NUMBER.getKey()).asText())
                                    );

            CLIENT_ID = config.path(PropertyKey.CLIENT_ID.getKey()).asText();
            TOPIC = config.path(PropertyKey.TOPIC.getKey()).asText();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // =================================================================================================================
    // Getter

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
        return IP_ADDRESS + ":" + getServicePort(serviceName);
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
    private static String getServicePort(String serviceName) {
        if (!PORT_MAP.containsKey(serviceName)) {
            log.warn("Check your port data");
            propertyIsNotExist(serviceName);
            throw new RuntimeException();
        }
        return PORT_MAP.get(serviceName);
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

    private static void propertyCheck(JsonNode config) {
        if (config.path(PropertyKey.IP_ADDRESS.getKey()).isMissingNode()) {
            propertyIsNotExist(PropertyKey.IP_ADDRESS.getKey());
            throw new RuntimeException();
        }
        if (config.path(PropertyKey.PORT.getKey()).isMissingNode()) {
            propertyIsNotExist(PropertyKey.PORT.getKey());
            throw new RuntimeException();
        }
        if (config.path(PropertyKey.MQTT.getKey()).isMissingNode()) {
            propertyIsNotExist(PropertyKey.MQTT.getKey());
            throw new RuntimeException();
        }
    }

    private static void propertyIsNotExist(String keyName) {
        log.warn("{} property value does not exist.", keyName);
    }
}
