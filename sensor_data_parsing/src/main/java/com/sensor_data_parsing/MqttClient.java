package com.sensor_data_parsing;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;

/**
 * MQTT 클라이언트를 생성, 메시지를 송수신, 서버에 연결하는 기능을 제공하는 클래스.
 * 합니다.
 */
public class MqttClient {
    private static final String DEFAULT_MQTT_HOST = "localhost"; // 기본 MQTT 호스트
    private static final int DEFAULT_MQTT_PORT = 8888; // 기본 MQTT 포트
    private static final String DEFAULT_MQTT_USER_NAME = ""; // 기본 사용자 이름
    private static final String DEFAULT_MQTT_PASSWORD = ""; // 기본 사용자 비밀번호
    private final Mqtt5Client client; // MQTT 클라이언트

    // MQTT 메시지를 수신했을 때 호출되는 콜백 인터페이스. (콜백 패턴)
    public interface Callback {
        void onMessageReceived(String message);
    }

    /**
     * 지정된 식별자, 호스트, 포트를 사용하여 MQTT 클라이언트를 초기화하고
     * 사용자 이름과 비밀번호를 사용하여 MQTT 서버에 연결합니다.
     *
     * @param identifier   클라이언트 식별자
     * @param mqttHost     MQTT 서버 호스트
     * @param port         MQTT 서버 포트
     * @param userName     사용자 이름
     * @param userPassword 사용자 비밀번호
     */
    public MqttClient(String identifier, String mqttHost, int port, String userName, String userPassword) {
        validateNonEmptyString(identifier, "식별자는 null이거나 비어 있을 수 없습니다.");
        validateNonEmptyString(mqttHost, "MQTT 호스트는 null이거나 비어 있을 수 없습니다.");
        if (port <= 0) {
            throw new IllegalArgumentException("포트는 0보다 커야 합니다.");
        }

        // MQTT 클라이언트를 초기화하고 설정
        this.client = Mqtt5Client.builder()
                .identifier(identifier) // 클라이언트 식별자 설정
                .serverHost(mqttHost)
                .automaticReconnectWithDefaultConfig() // 자동 재연결 설정
                .serverPort(port)
                .build();

        // 사용자 인증을 통해 MQTT 서버에 연결
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
     * 기본 사용자 이름과 비밀번호를 사용하여 지정된 식별자, 호스트, 포트를 통해
     * MQTT 클라이언트를 초기화합니다.
     *
     * @param identifier 클라이언트 식별자 (MQTT 클라이언트를 구분하는 고유 ID)
     * @param mqttHost   MQTT 서버 호스트 (연결할 서버의 주소)
     * @param port       MQTT 서버 포트 (연결할 서버의 포트 번호)
     */
    public MqttClient(String identifier, String mqttHost, int port) {
        this(identifier, mqttHost, port, DEFAULT_MQTT_USER_NAME, DEFAULT_MQTT_PASSWORD);
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
        validateNonEmptyString(topic, "토픽은 null이거나 비어 있을 수 없습니다.");
        validateNonEmptyString(message, "메시지는 null이거나 비어 있을 수 없습니다.");

        this.client.toAsync().publishWith()
                .topic(topic)
                .payload(message.getBytes(StandardCharsets.UTF_8))
                .qos(MqttQos.AT_LEAST_ONCE) // QoS 1 (최소 한 번 전송)
                .send();
    }

    /**
     * 지정된 토픽에 대한 구독을 시작하고, 메시지를 수신하면 콜백을 호출합니다.
     *
     * @param topicFilter 구독할 토픽 필터
     * @param callback    메시지를 수신할 때 호출되는 콜백
     */
    public void subscribeMessage(String topicFilter, Callback callback) {
        this.client.toAsync().subscribeWith()
                .topicFilter(topicFilter)
                .callback(publish -> {
                    String message = new String(publish.getPayloadAsBytes(),
                            StandardCharsets.UTF_8);
                    callback.onMessageReceived(message);
                }).send();
    }

    /**
     * 문자열이 null이거나 비어 있는지 확인하는 유틸리티 메서드.
     *
     * @param value        검사할 문자열
     * @param errorMessage 예외 발생 시 사용할 오류 메시지
     */
    private void validateNonEmptyString(String value, String errorMessage) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
