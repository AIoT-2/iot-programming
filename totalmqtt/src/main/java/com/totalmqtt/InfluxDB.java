package com.totalmqtt;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;

import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
public class InfluxDB implements Consumer{
    private String influxDBIp;
    private int influxDBPort;
    // org Name (초기 설정에서 지정한 Organization Name)
    private static String ORG = "nhnacademy";
    // bucket name
    private static String BUCKET = "mqtt_modbus";
    // bucket Token
    private static char[] TOKEN = "Gksj8LnOLhrj4KJHPWza8OppXW1MO9TMpg8yElQZ1N4Lc3X3O4lbSyhSye58qkwPJ-K7bC2fXHDBJ2vKgaLS1g=="
            .toCharArray(); // 고치기

    public InfluxDB() {
        influxDBIp = "192.168.71.205";
        influxDBPort = 8086;
    }

    public void settingInformation(String ip, int port){
        this.influxDBIp = ip;
        this.influxDBPort = port;
    }


    @Override
    public void connect() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'connect'");
    }

    @Override
    public void execute(String jsonData) {
        try {
            InfluxDBClient influxDBClient = InfluxDBClientFactory.create("http://"+influxDBIp+":"+influxDBPort, TOKEN, ORG, BUCKET);

            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            ObjectMapper objectMapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(jsonData, Map.class);

            // Point 객체 생성
            Point point = null;
            if(data.get("deviceName") == null){
                point = Point.measurement(data.get("name").toString())
                .time(System.currentTimeMillis(), WritePrecision.MS);
            }
            else{
                point = Point.measurement(data.get("deviceName").toString())
                .time(System.currentTimeMillis(), WritePrecision.MS);
            }

            // Map을 순회하여 필드를 추가
            point.addFields(data);

            writeApi.writePoint(point);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
