package com.nhnacademy.util;

/**
 * {@code /resources/config.json} 내부에 Key 값을 매핑할 목적
 */
public enum PropertyKey {

    TIME_FORMAT("TIME_FORMAT"),

    /**
     * {@code config.json} 파일 경로
     */
    CONFIG_PATH("/config.json"),

    // ==============================================

    /**
     * 네트워크 설정 값의 집합
     */
    NETWORK_CONFIG("NETWORK_CONFIG"),

    // ----------------------------------------------

    /**
     * IP 주소
     */
    IP_ADDRESS("IP_ADDRESS"),

    /**
     * IP 주소에 해당하는 서비스 목록
     */
    PORT_LIST("PORT_LIST"),
    SERVICE_NAME("SERVICE_NAME"),
    PORT_NUMBER("PORT_NUMBER"),

    // ==============================================

    /**
     * MQTT 설정 값의 집합
     */
    MQTT_CONFIG("MQTT_CONFIG"),

    // ----------------------------------------------

    /**
     * 클라이언트 ID
     */
    CLIENT_ID("CLIENT_ID"),

    /**
     * 구독 및 발행 주제
     */
    TOPIC("TOPIC");

    // ==============================================

    private final String key;

    PropertyKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
