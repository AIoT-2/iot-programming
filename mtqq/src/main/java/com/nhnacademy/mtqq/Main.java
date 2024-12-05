package com.nhnacademy.mtqq;

import com.nhnacademy.mtqq.Interface.TransForMqtt;
import com.nhnacademy.mtqq.handler.ModbusHandler;
import com.nhnacademy.mtqq.handler.TCPHandler;
import com.nhnacademy.mtqq.mqtt.MqttToInfluxDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class Main {
    //TODO #0. 파라미터 체크하기    ---> OK!
    //TODO #1. MessageData대신 map으로 데이터 받아오기!
    //TODO #2. 디폴트 값을 설정하기! -> 현재 파일에서 config나 conf로 디폴트값을 정해줄 것.   ---> 50%
    //TODO #3. Handler.handle() 이름 변경하거나 return하는 값을 object로 하기, 메소드와 행동이 일치하지 않음!  ---> OK?
    //TODO #4. token값을 환경변수로 주지 말고 properties로 줄 것. ---> OK!

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            // 사용하려는 데이터 소스를 선택 (Modbus 또는 TCP)
            TransForMqtt transForMqtt = getDataSourceHandler("Modbus");

            // 데이터 처리 및 MQTT 메시지 생성
            Map<String, Object> mqttMessage = transForMqtt.transFromMqttMessage();
            log.info("mqttMessage: {}", mqttMessage);

            // MQTT 메시지를 InfluxDB에 저장
            MqttToInfluxDB mqttToInfluxDB = new MqttToInfluxDB();
            mqttToInfluxDB.run(mqttMessage);  // Map<String, Object> 형태로 전달

        } catch (Exception e) {
            log.error("Error processing the message", e);
        }
    }

    private static TransForMqtt getDataSourceHandler(String sourceType) {
        if ("Modbus".equalsIgnoreCase(sourceType)) {
            return new ModbusHandler(); // ModbusHandler가 TransForMqtt를 구현한 클래스여야 함
        } else if ("TCP".equalsIgnoreCase(sourceType)) {
            return new TCPHandler();  // TCPHandler가 TransForMqtt를 구현한 클래스여야 함
        } else {
            throw new IllegalArgumentException("Invalid source type: " + sourceType);
        }
    }

}
