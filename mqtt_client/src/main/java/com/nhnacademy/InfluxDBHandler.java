package com.nhnacademy;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.write.Point;

public class InfluxDBHandler {

    private static final String URL = "http://192.168.71.204:8086";
    private static final String TOKEN = "my-secret-token"; // InfluxDB 2.x API Token
    private static final String ORG = "yeong"; // InfluxDB organization 이름
    private static final String BUCKET = "java_influx"; // InfluxDB bucket 이름

    private final InfluxDBClient influxDBClient;
    private final WriteApiBlocking writeApiBlocking;

    public InfluxDBHandler() {
        // InfluxDB 2.x 클라이언트 초기화
        influxDBClient = InfluxDBClientFactory.create(URL, TOKEN.toCharArray(), ORG, BUCKET);
        writeApiBlocking = influxDBClient.getWriteApiBlocking();
    }

    public void writeData(Point point) {
        try {
            // 데이터 쓰기
            writeApiBlocking.writePoint(point);
            System.out.println("Data written to InfluxDB: " + point.toString());
        } catch (Exception e) {
            System.err.println("Error writing to InfluxDB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void close() {
        ((InfluxDBClient) writeApiBlocking).close();  // 명시적으로 close 호출
        influxDBClient.close();
    }
}
