package com.totalmqtt;

import java.io.File;

//1. mqtt의 topic 설정
//2. Pub제작
//3. ModBus
public class Main {
    
    public static void main(String[] args) {

        // 1. json을 받을 수 있도록 설정
        // 2. 3개가 묶일 수 있도록 하나의 최상위 클래스 제작
        AddressJsonParser addressJsonParser = new AddressJsonParser();
        addressJsonParser.setJsonLocation("./totalmqtt/src/main/java/com/totalmqtt/address.json");
        addressJsonParser.parsing();
 
        Modbus2 modbus2 = new Modbus2();
        modbus2.settingInformation("192.168.70.203", 502);
        modbus2.settingIterator(100, 2400, 100);
        Thread thModbus = new Thread(modbus2);

        thModbus.start(); // 5초 마다 갱신

        MqttToData recvMqtt = new MqttToData();
        recvMqtt.connect();
        recvMqtt.messageSend();

        InfluxDB influxDB = new InfluxDB();
        Thread thInfluxDB = new Thread(influxDB);

        thInfluxDB.start(); // 5초 마다 갱신
    }
}