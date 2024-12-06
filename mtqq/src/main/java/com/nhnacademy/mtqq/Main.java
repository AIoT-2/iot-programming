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
        // ModbusHandler 및 MqttToInfluxDB 인스턴스 생성
        ModbusHandler modbusHandler = new ModbusHandler();
        MqttToInfluxDB mqttToInfluxDB = new MqttToInfluxDB();

        // Thread1: ModbusHandler로 데이터를 읽고 MQTT 메시지로 변환
        Thread thread1 = new Thread(() -> {
            while (true) {
                try {
                    // Modbus 데이터를 읽어서 변환
                    Map<String, Map<Integer, Double>> locationData = modbusHandler.readData();
                    if (locationData != null && !locationData.isEmpty()) {
                        Map<String, Object> mqttMessageData = modbusHandler.transFromMqttMessage(locationData);

                        // MQTT 메시지를 MqttToInfluxDB로 전달
                        mqttToInfluxDB.run(mqttMessageData);
                    }

                    // 5초 간격으로 데이터 수집
                    Thread.sleep(5000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // Thread2: MQTT 메시지에서 데이터 처리 및 InfluxDB로 저장
        Thread thread2 = new Thread(() -> {
            while (true) {
                try {
                    // MQTT 데이터에서 InfluxDB로 저장하는 작업 수행
                    mqttToInfluxDB.run(null); // 실제 메시지 데이터를 받지 않을 경우 run 메서드에서 로직 변경 필요
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // 스레드 실행
        thread1.start();
        thread2.start();

        // 메인 스레드에서 스레드 관리
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }
}
