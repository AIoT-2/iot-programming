package com.sensor_data_flow.threads;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.sensor_data_flow.MqttClient;

// MQTT로부터 데이터를 받아 InfluxDB에 저장하는 작업을 수행하는 클래스.
public class MqttToInfluxDB implements Runnable {
    private static final ObjectMapper objectMapper = new ObjectMapper(); // JSON 처리에 사용할 ObjectMapper 객체

    private static final String DEFAULT_INFLUXDB_URL = "http://192.168.71.210:8086"; // 기본 InfluxDB 서버 URL
    // 기본 Token
    private static final String DEFAULT_INFLUXDB_TOKEN = "YKnMLcHEQ3wsyg778dUXYzvGijjmR4ImSVOYU2Nlr5BugSyFGjNsTyRb6c-5eozXGLTObSxNWqGW5yaUQxRaWw==";
    private static final String DEFAULT_INFLUXDB_ORGANIZATION = "nhnacademy_010"; // 기본 Organization 이름
    private static final String DEFAULT_INFLUXDB_BUCKET = "test"; // 기본 Bucket 이름

    // MQTT 클라이언트와 InfluxDB 클라이언트
    private final MqttClient subscriber;
    private final InfluxDBClient influxDBClient;

    /**
     * MQTT 클라이언트와 InfluxDB 클라이언트를 초기화하는 생성자.
     * 
     * @param subscriber     MQTT 구독을 위한 클라이언트
     * @param influxDBClient InfluxDB 클라이언트
     */
    public MqttToInfluxDB(MqttClient subscriber, InfluxDBClient influxDBClient) {
        this.subscriber = subscriber;
        this.influxDBClient = influxDBClient;
    }

    /**
     * MQTT 클라이언트만 제공하는 생성자. InfluxDB 클라이언트는 기본 설정으로 생성됩니다.
     * 
     * @param subscriber MQTT 구독을 위한 클라이언트
     */
    public MqttToInfluxDB(MqttClient subscriber) {
        this(subscriber, InfluxDBClientFactory.create(DEFAULT_INFLUXDB_URL, DEFAULT_INFLUXDB_TOKEN.toCharArray(),
                DEFAULT_INFLUXDB_ORGANIZATION, DEFAULT_INFLUXDB_BUCKET));
    }

    // MQTT로부터 메시지를 수신하고 해당 메시지를 InfluxDB에 저장하는 작업을 수행합니다.
    @Override
    public void run() {
        // MQTT 토픽 구독 및 메시지 수신
        subscriber.subscribeMessage("application/#", (String message) -> {
            try {
                JsonNode rootNode = objectMapper.readTree(message);

                // deviceName과 spotName 추출
                String deviceName = rootNode.path("deviceName").asText();
                String spotName = rootNode.path("spotName").asText();
                JsonNode spotNode = rootNode.path("spotName");

                System.out.println("deviceName: " + deviceName);
                System.out.println("spotName: " + spotName);

                JsonNode dataNode = rootNode.path("data");

                // 데이터를 Map으로 변환
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = objectMapper
                        .readValue(dataNode.toString(), Map.class);

                // 데이터 출력
                System.out.println("----------data----------");
                for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
                    System.out.println(entry.getKey() + ": "
                            + entry.getValue());
                }

                // InfluxDB에 데이터를 저장
                Point point = Point.measurement(deviceName);
                if (!spotNode.isMissingNode()) {
                    point.addTag("spotName", spotName);
                }
                point.addFields(dataMap);
                point.time(System.currentTimeMillis(),
                        WritePrecision.MS); // 타임스탬프 설정
                influxDBClient.getWriteApiBlocking().writePoint(point); // 데이터 삽입

                System.out.println("데이터가 InfluxDB에 성공적으로 저장되었습니다!\n");

            } catch (IOException e) {
                System.err.println("JSON 파싱 중 오류가 발생: " + e.getMessage());
            }
        });
    }
}
