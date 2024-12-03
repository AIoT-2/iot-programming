package com.nhnacademy.mtqq;

import com.nhnacademy.mtqq.Interface.DataSourceHandler;
import com.nhnacademy.mtqq.handler.ModbusHandler;
import com.nhnacademy.mtqq.handler.TCPHandler;
import com.nhnacademy.mtqq.mtqq.MqttToInfluxDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            // 사용하려는 데이터 소스를 선택 (Modbus 또는 TCP)
            DataSourceHandler dataSourceHandler = getDataSourceHandler("Modbus");

            // 데이터 처리 및 MQTT 메시지 생성
            String mqttMessage = dataSourceHandler.handle();
            log.info("mqttMessage: {}", mqttMessage);

            // MQTT 메시지를 InfluxDB에 저장
            MqttToInfluxDB mqttToInfluxDB = new MqttToInfluxDB();
            mqttToInfluxDB.run(mqttMessage);

        } catch (Exception e) {
            log.error("Error processing the message", e);
        }
    }

    private static DataSourceHandler getDataSourceHandler(String sourceType) {
        if ("Modbus".equalsIgnoreCase(sourceType)) {
            return new ModbusHandler();
        } else if ("TCP".equalsIgnoreCase(sourceType)) {
            return new TCPHandler();
        } else {
            throw new IllegalArgumentException("Invalid source type: " + sourceType);
        }
    }
}
