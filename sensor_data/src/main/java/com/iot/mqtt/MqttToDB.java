package com.iot.mqtt;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.write.Point;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttToDB {

    static final Logger logger = LoggerFactory.getLogger(MqttToDB.class);

    private static final String URL = "http://192.168.71.207:8086";
    private static final String TOKEN = "aCden7eIjcqbw504Yp7gzHJsdozMJS9E-HqOlm6dKPCoQyp60OWVohL-ctZgFlkgMDiGWAaRLma5oQahCkIPiA=="; // token
    private static final String ORG = "123123";
    private static final String BUCKET = "abc";

    private InfluxDBClient influxDBClient;

    public MqttToDB() {
        this.influxDBClient = InfluxDBClientFactory.create(URL, TOKEN.toCharArray(), ORG, BUCKET);
    }

    public void writeToDB(Point point) {
        try {
            influxDBClient.getWriteApiBlocking().writePoint(point);
            logger.info("Data written to InfluxDB: {}", point);

        } catch (Exception e) {
            logger.error("ERROR writing data to InfluxDB", e);
        }
    }

    public void close() {
        if (influxDBClient != null) {
            influxDBClient.close();
            logger.info("InfluxDB connection closed.");
        }
    }
}
