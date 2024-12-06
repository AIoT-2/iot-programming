package com.totalmqtt;

import java.io.File;

import java.util.*;

//1. mqtt의 topic 설정
//2. Pub제작
//3. ModBus
public class Main {
    
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {

        // 1. json을 받을 수 있도록 설정
        // 2. 3개가 묶일 수 있도록 하나의 최상위 클래스 제작
        AddressJsonParser addressJsonParser = new AddressJsonParser();
        addressJsonParser.setJsonLocation("./totalmqtt/src/main/java/com/totalmqtt/address.json");
        addressJsonParser.parsing();
 
        Modbus2 modbus2 = new Modbus2();
        Map<String, Object> modbusAddress = addressJsonParser.getAddress("modbusToJava");
        modbus2.settingInformation(modbusAddress.get("ip").toString(), Integer.parseInt(modbusAddress.get("port").toString()));
        modbus2.settingIterator(100, 2400, 100);
        Thread thModbus = new Thread(modbus2);

        
        MqttToData recvMqtt = new MqttToData();
        Map<String, Object> mqttAddress = addressJsonParser.getAddress("brokerToJava");
        recvMqtt.settingInformation(mqttAddress.get("ip").toString(), Integer.parseInt(mqttAddress.get("port").toString()));
        recvMqtt.connect();
        recvMqtt.messageSend();
        
        InfluxDB influxDB = new InfluxDB();
        Map<String, Object> influxdbAddress = addressJsonParser.getAddress("JavaToInfluxDB");
        Map<String, Object> brokerAddress = addressJsonParser.getAddress("JavaTobroker");
        influxDB.influxDBInformation(influxdbAddress.get("ip").toString(), Integer.parseInt(influxdbAddress.get("port").toString()));
        influxDB.mqttInformation(brokerAddress.get("ip").toString(), Integer.parseInt(brokerAddress.get("port").toString()));
        Thread thInfluxDB = new Thread(influxDB);
        
        thModbus.start(); // 5초 마다 갱신
        thInfluxDB.start(); // 5초 마다 갱신
    }
}