package com.nhnacademy.influxdb;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.nhnacademy.config.InfluxdbConfig;
import lombok.extern.slf4j.Slf4j;
import java.time.Instant;
import java.util.Map;

@Slf4j
public class InfluxDBWriter {
    private InfluxDBClient influxDBClient;
    private WriteApiBlocking writeApi;

    // InfluxDBConfig와 topics, buckets 리스트를 생성자에서 받아 설정
    public InfluxDBWriter(InfluxdbConfig influxDBConfig) {
        // InfluxDB 설정값을 사용하여 클라이언트 생성
        log.info("create influxdb client");
        this.influxDBClient = InfluxDBClientFactory.create(influxDBConfig.getUrl(),
                influxDBConfig.getToken().toCharArray(), influxDBConfig.getOrg(),
                influxDBConfig.getBucket());
        this.writeApi = influxDBClient.getWriteApiBlocking();
    }

    public void writeToInfluxDB(Map<String, String> topicMap, String measurement, long timeMillis,
            double value) {
        // 데이터 포인트 작성 코드
        Point point = Point.measurement(measurement).addTags(topicMap).addField("value", value)
                .time(Instant.ofEpochMilli(timeMillis), WritePrecision.MS);

        // InfluxDB에 데이터 저장
        writeApi.writePoint(point);
        log.info("Data written to InfluxDB: {}", point);
    }

    public void close() {
        influxDBClient.close();
    }
}
