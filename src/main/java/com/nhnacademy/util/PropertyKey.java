package com.nhnacademy.util;

public enum PropertyKey {
    /**
     * {@code config.json} 파일 경로
     */
    CONFIG_PATH("/config.json"),
    CONFIG("CONFIG"),
    SERVICE("SERVICE"),
    NUMBER("NUMBER"),
    IP_ADDRESS("IP_ADDRESS"),
    PORT("PORT"),
    MQTT("MQTT"),
    CLIENT_ID("CLIENT_ID"),
    TOPIC("TOPIC");

    private final String key;

    PropertyKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
