package com.sensor_data_parsing.interfaces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ProtocolToMqtt extends Runnable {
    static final Logger logger = LoggerFactory.getLogger(ProtocolToMqtt.class);

    /**
     * 프로토콜로부터 데이터를 읽어오는 메서드.
     * 프로토콜별로 데이터를 어떻게 가져오는지 구현할 수 있는 추상 메서드입니다.
     *
     * @return 프로토콜로부터 읽어온 데이터
     */
    String fetchDataFromProtocol();

    /**
     * 프로토콜 데이터의 형식을 변환하여 MQTT에 맞게 변환하는 메서드.
     * 이 메서드는 프로토콜 데이터를 MQTT 메시지 포맷으로 변환합니다.
     *
     * @param data 프로토콜에서 받은 원본 데이터
     * @return 변환된 MQTT 메시지
     */
    String[] convertToMqttFormat(String data);

    /**
     * 프로토콜로부터 데이터를 수신하고, MQTT로 변환하여 발송하는 메서드.
     * 프로토콜과 관련된 메시지를 처리하고 이를 MQTT 메시지로 변환하여 전송합니다.
     *
     * @param topic   전송할 MQTT 토픽
     * @param message 프로토콜에서 수신한 메시지
     */
    void sendMessageToMqtt(String[] message);

    @Override
    default void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                String data = fetchDataFromProtocol();
                String[] convertData = convertToMqttFormat(data);
                sendMessageToMqtt(convertData);
            } catch (IllegalArgumentException e) {
                // 예외 발생 시 로그 기록
                logger.error("Invalid argument encountered: ", e);
            } catch (Exception e) {
                // 예외 발생 시 일반적인 예외를 잡아서 로그 기록
                logger.error("Unexpected error occurred: ", e);
            }
        }
    }
}
