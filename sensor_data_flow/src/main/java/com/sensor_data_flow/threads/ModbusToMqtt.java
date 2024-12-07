package com.sensor_data_flow.threads;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.msg.response.ReadInputRegistersResponse;
import com.sensor_data_flow.client.ModbusTcpClient;
import com.sensor_data_flow.client.MqttClient;
import com.sensor_data_flow.interfaces.ProtocolToMqtt;

/**
 * ModbusToMqtt 클래스는 Modbus 메시지를 구독하고, 해당 메시지를 처리한 후 새로운 형식으로 발행하는 작업을 수행합니다.
 * Runnable 인터페이스를 구현하여 별도의 스레드에서 실행됩니다.
 */
public class ModbusToMqtt implements ProtocolToMqtt {
    private static final Logger logger = LoggerFactory.getLogger(ModbusToMqtt.class);
    private static final ObjectMapper objectMapper = new ObjectMapper(); // JSON 처리에 사용할 ObjectMapper 객체

    private final MqttClient publisher; // MQTT 클라이언트
    private final ModbusTcpClient subscriber; // Modbus 연결을 위한 ModbusConnect 객체

    /**
     * ModbusToMqtt 클래스의 생성자
     * 
     * @param publisher  MQTT 메시지를 발행할 MqttClient 객체
     * @param subscriber Modbus 데이터를 구독할 ModbusConnect 객체
     */
    public ModbusToMqtt(MqttClient publisher, ModbusTcpClient subscriber) {
        this.publisher = publisher;
        this.subscriber = subscriber;
    }

    @Override
    public void run() {
        try {
            if (!subscriber.getModbusMaster().isConnected()) {
                subscriber.getModbusMaster().connect();
            }

            while (!Thread.currentThread().isInterrupted()) {
                String data = fetchDataFromProtocol();
                String[] convertData = convertToMqttFormat(data);
                sendMessageToMqtt(convertData);

                // 30초 대기
                waitForNextCycle(30);
            }
        } catch (ModbusIOException e) {
            logger.error("Modbus 연결 오류: {}", e.getMessage());
        } finally {
            // 스레드 종료 시 연결 해제
            try {
                if (subscriber.getModbusMaster().isConnected()) {
                    subscriber.getModbusMaster().disconnect();
                }
            } catch (ModbusIOException e) {
                logger.error("Modbus 연결 해제 오류: {}", e.getMessage());
            }
        }
    }

    @Override
    public String fetchDataFromProtocol() {
        ReadInputRegistersResponse response = null;
        try {
            response = subscriber.fetchModbusData();
        } catch (ModbusProtocolException e) {
            logger.error("Modbus 프로토콜 오류: {}", e.getMessage());
        } catch (ModbusIOException e) {
            logger.error("Modbus IO 오류: {}", e.getMessage());
        }

        Map<String, Object> dataMap = subscriber.getDataMap(response);

        Map<String, Object> dataMessage = new HashMap<>();
        dataMessage.put("deviceName", subscriber.getChannelLocation());
        dataMessage.put("data", dataMap);

        return convertToJson(dataMessage);
    }

    @Override
    public String[] convertToMqttFormat(String message) {
        return new String[] { "application/modbus", message };
    }

    @Override
    public void sendMessageToMqtt(String[] message) {
        publisher.sendMessage(message[0], message[1]);
    }

    /**
     * 30초 동안 대기하는 메소드
     */
    public void waitForNextCycle(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("스레드 대기 중 인터럽트 발생: " + e.getMessage());
        }
    }

    /**
     * 데이터를 JSON 문자열로 변환하는 메소드
     *
     * @param dataMessage 변환할 데이터
     * @return JSON 문자열
     */
    private String convertToJson(Map<String, Object> dataMessage) {
        try {
            return objectMapper.writeValueAsString(dataMessage);
        } catch (JsonProcessingException e) {
            logger.error("메시지 JSON 변환 오류: {}", e.getMessage());
            return null; // 변환 실패 시 null 반환
        }
    }
}
