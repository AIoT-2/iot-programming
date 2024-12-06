package com.nhnacademy.mtqq.handler;

import com.nhnacademy.mtqq.Interface.TransForMqtt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class TCPHandler implements TransForMqtt {
    private static final Logger log = LoggerFactory.getLogger(TCPHandler.class);

    private String host;
    private int port;

    // 생성자
    public TCPHandler(String host, int port) {
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("host 값을 입력하세요.");
        }
        if (port <= 0) {
            throw new IllegalArgumentException("port 값은 양수여야 합니다.");
        }
        this.host = host;
        this.port = port;
    }

    // 기본 생성자 (기본 TCP 서버 정보 설정)
    public TCPHandler() {
        this.host = "127.0.0.1"; // 기본 호스트
        this.port = 5000; // 기본 포트
    }

    // TCP 소켓을 통해 데이터를 읽는 메서드
    public Map<String, String> readData() {
        Map<String, String> dataMap = new HashMap<>();
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // 서버로 요청 보내기
            out.println("REQUEST_DATA");
            log.info("Sent request to server: REQUEST_DATA");

            // 서버로부터 데이터 읽기
            String responseLine;
            while ((responseLine = in.readLine()) != null) {
                log.info("Received data: {}", responseLine);

                // 데이터를 key-value 형식으로 파싱 (예: "key:value" 형식 가정)
                String[] keyValue = responseLine.split(":");
                if (keyValue.length == 2) {
                    dataMap.put(keyValue[0].trim(), keyValue[1].trim());
                }
            }
        } catch (Exception e) {
            log.error("Error during TCP data reading", e);
        }
        return dataMap;
    }

    @Override
    public Map<String, Object> transFromMqttMessage(Map<String, Map<Integer, Double>> locationData) {
        if (locationData.isEmpty()) {
            throw new IllegalArgumentException("locationData 값이 없습니다.");
        }

        Map<String, Object> mqttMessageData = new HashMap<>();

        // locationData 순회
        for (Map.Entry<String, Map<Integer, Double>> entry : locationData.entrySet()) {
            String key = entry.getKey(); // key는 String
            Map<Integer, Double> value = entry.getValue(); // value는 Map<Integer, Double>

            // value 내부 데이터를 가공
            Map<String, Double> processedData = new HashMap<>();
            for (Map.Entry<Integer, Double> innerEntry : value.entrySet()) {
                // Integer 키를 문자열로 변환하여 Map<String, Double>에 저장
                processedData.put(String.valueOf(innerEntry.getKey()), innerEntry.getValue());
            }

            // 처리된 데이터를 MQTT 메시지 형식으로 추가
            mqttMessageData.put(key, processedData);
        }

        // 디바이스 이름 추가
        mqttMessageData.put("deviceName", "TCPDevice");
        return mqttMessageData;
    }
}
