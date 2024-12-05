package com.sensor_data_parsing;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;

/**
 * MQTT 클라이언트를 생성하고 관리하는 클래스.
 * 메시지를 전송하고, 서버에 연결하는 기능을 제공합니다.
 */
public class MqttClient {
    private static final String DEFAULT_MQTT_HOST = "localhost"; // 기본 MQTT 호스트
    private static final int DEFAULT_MQTT_PORT = 8888; // 기본 MQTT 포트
    private Mqtt5Client client; // MQTT 클라이언트

    /**
     * 지정된 식별자, 호스트, 포트를 사용하여 MQTT 클라이언트를 초기화합니다.
     *
     * @param identifier 클라이언트 식별자
     * @param mqttHost   MQTT 서버 호스트
     * @param port       MQTT 서버 포트
     */
    public MqttClient(String identifier, String mqttHost, int port) {
        validateString(identifier, "식별자는 null이거나 비어 있을 수 없습니다.");
        validateString(mqttHost, "MQTT 호스트는 null이거나 비어 있을 수 없습니다.");
        if (port <= 0) {
            throw new IllegalArgumentException("포트는 0보다 커야 합니다.");
        }

        this.client = Mqtt5Client.builder()
                .identifier(identifier) // 클라이언트 식별자 설정
                .serverHost(mqttHost)
                .automaticReconnectWithDefaultConfig() // 자동 재연결 설정
                .serverPort(port)
                .build();
    }

    /**
     * 기본 호스트와 포트를 사용하여 MQTT 클라이언트를 초기화합니다.
     *
     * @param identifier 클라이언트 식별자
     */
    public MqttClient(String identifier) {
        this(identifier, DEFAULT_MQTT_HOST, DEFAULT_MQTT_PORT);
    }

    /**
     * 지정된 토픽으로 메시지를 전송합니다.
     *
     * @param topic   메시지를 보낼 토픽
     * @param message 전송할 메시지
     */
    public void sendMessage(String topic, String message) {
        validateString(topic, "토픽은 null이거나 비어 있을 수 없습니다.");
        validateString(message, "메시지는 null이거나 비어 있을 수 없습니다.");

        this.client.toAsync().publishWith()
                .topic(topic)
                .payload(message.getBytes(StandardCharsets.UTF_8))
                .qos(MqttQos.AT_LEAST_ONCE) // QoS 1 (최소 한 번 전송)
                .send();
    }

    /**
     * 사용자 이름과 비밀번호를 사용하여 MQTT 서버에 연결합니다.
     *
     * @param userName     사용자 이름
     * @param userPassword 사용자 비밀번호
     */
    public void connectToMqtt(String userName, String userPassword) {
        validateString(userName, "사용자 이름은 null이거나 비어 있을 수 없습니다.");
        validateString(userPassword, "비밀번호는 null이거나 비어 있을 수 없습니다.");

        this.client.toBlocking().connectWith()
                .simpleAuth()
                .username(userName)
                .password(userPassword.getBytes(StandardCharsets.UTF_8))
                .applySimpleAuth()
                .cleanStart(false)
                .sessionExpiryInterval(TimeUnit.HOURS.toSeconds(1))
                .send();
    }

    /**
     * 문자열이 null이거나 비어 있는지 확인하는 유틸리티 메서드.
     *
     * @param value        검사할 문자열
     * @param errorMessage 예외 발생 시 사용할 오류 메시지
     */
    private void validateString(String value, String errorMessage) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
