package com.totalmqtt;

public class Main2 {
    public static void main(String[] args) {
        InfluxDB influxDB = new InfluxDB();
        Thread thInfluxDB = new Thread(influxDB);

        thInfluxDB.start(); // 5초 마다 갱신
    }
}
