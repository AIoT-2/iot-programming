package com.nhnacademy.settings;

// 클래스들의 디폴트 고정 값들 입니다.
public final class DemoSetting {
    public static final String BROKER = "tcp://192.168.71.202:1883";
    public static final String CLIENT_ID = "atgn002"; // 클라이언트 ID

    public static final String MQTT_TOPIC = "application/#"; // 구독 및 발행 주제
    public static final String MODBUS_TOPIC = "application/modbus"; // 발행 주제

    public static final String INFLUXDB_URL = "http://192.168.71.202:8086";
    public static final String INFLUXDB_TOKEN = "b7KKn-OWOYSt7FwtqZBPRSVWg5qaHJOSNUYMO8t3CO0A38hzEnUCsAzS7ADO8NhcA6EVp44F4Yh-nm5lt40ZZw==";
    public static final String INFLUXDB_ORG = "root";
    public static final String INFLUXDB_BUCKET = "test";

    private DemoSetting() {
    }
}
