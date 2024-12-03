package com.totalmqtt;


//1. mqtt의 topic 설정
//2. Pub제작
//3. ModBus
public class Main {
    
    public static void main(String[] args) {
        Modbus2 modbus2 = new Modbus2();
        modbus2.setting_iterator(100, 2400, 100);
        Thread th_modbus = new Thread(modbus2);
        th_modbus.start(); // 5초 마다 갱신

        RecvMqtt recvMqtt = new RecvMqtt();
        recvMqtt.connect();
        recvMqtt.MSGSend();

    }
}