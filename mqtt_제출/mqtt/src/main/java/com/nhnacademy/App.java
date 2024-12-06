package com.nhnacademy;

import org.eclipse.paho.client.mqttv3.MqttException;
import com.nhnacademy.config.AppConfig;
import com.nhnacademy.config.ConfigLoader;
import com.nhnacademy.config.InfluxdbConfig;
import com.nhnacademy.config.ModbusConfig;
import com.nhnacademy.config.MqttConfig;
import com.nhnacademy.mqtt.Pub;
import com.nhnacademy.mqtt.Sub;
import com.serotonin.modbus4j.exception.ModbusInitException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class App {
    private static AppConfig config;

    public static void main(String[] args) {
        try {
            log.info("설정 파일 로드 중...");
            config = ConfigLoader.loadConfig("config.yml");

            // 설정 정보 확인
            log.info("MQTT Broker: {}", config.getMqtt().getBroker());
            log.info("Influxdb URL: {}", config.getInfluxdb().getUrl());
            log.info("Modbus Host: {}", config.getModbus().getHost());

            // 각 모듈 초기화
            initMqtt();
            initInfluxdb();
            initModbus();
        } catch (Exception e) {
            log.debug("Application initialization failed: {}", e.getMessage());
            e.printStackTrace();
        }

        Thread subThread = new Thread(new Sub(config.getMqtt(), config.getInfluxdb()));
        subThread.start();
        try {
            Thread pubThread = new Thread(new Pub(config.getMqtt(), config.getModbus()));
            pubThread.start();
        } catch (MqttException me) {
            me.printStackTrace();
        } catch (ModbusInitException mie) {
            mie.printStackTrace();
        }
    }

    private static void initMqtt() {
        MqttConfig mqttConfig = config.getMqtt();
        log.info("Initializing MQTT client...");
        log.info("Broker: " + mqttConfig.getBroker());
        log.info("Client ID: " + mqttConfig.getClientId());
        log.info("Topics: " + mqttConfig.getTopics());
    }

    private static void initInfluxdb() {
        InfluxdbConfig influxDbConfig = config.getInfluxdb();
        log.info("Initializing InfluxDB client...");
        log.info("URL: " + influxDbConfig.getUrl());
        log.info("Org: " + influxDbConfig.getOrg());
        log.info("Bucket: " + influxDbConfig.getBucket());
    }

    private static void initModbus() {
        ModbusConfig modbusConfig = config.getModbus();
        log.info("Initializing Modbus client...");
        log.info("Host: " + modbusConfig.getHost());
        log.info("Port: " + modbusConfig.getPort());
        log.info("Unit ID: " + modbusConfig.getSlaveId());
    }
}
