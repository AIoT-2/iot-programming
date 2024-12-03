package com.example.influxdb_mqtt;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;

public class InfluxDBConnector {
    private static final String INFLUXDB_URL = "http://192.168.71.211:8086"; // InfluxDB URL
    private static final String TOKEN = "xlS-359_BM9sGe33acfHfCZy5XT17Q73WMRnIujijpzXFfo2GXmP0lwY0KLVK2lEOil9SYIsQWRretha7JB9Ig=="; // InfluxDB 토큰
    private static final String ORG = "pi"; // 조직 이름
    private static final String BUCKET = "telegraf"; // 버킷 이름

    private static InfluxDBClient influxDBClient;

    public static void initialize() {
        influxDBClient = InfluxDBClientFactory.create(INFLUXDB_URL, TOKEN.toCharArray(), ORG, BUCKET);
    }

    public static void writeData(String measurement, String field, double value) {
        try (WriteApi writeApi = influxDBClient.getWriteApi()) {
            String data = String.format("%s %s=%f", measurement, field, value);
            writeApi.writeRecord(WritePrecision.NS, data);
        }
    }

    public static void close() {
        if (influxDBClient != null) {
            influxDBClient.close();
        }
    }
}
