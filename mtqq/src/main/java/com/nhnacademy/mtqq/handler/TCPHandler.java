package com.nhnacademy.mtqq.handler;

import java.io.*;
import java.net.Socket;

import com.nhnacademy.mtqq.Interface.DataSourceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TCPHandler implements DataSourceHandler {

    private static final Logger log = LoggerFactory.getLogger(TCPHandler.class);
    private String HOST = "192.168.70.203"; // TCP 서버 IP
    private int PORT = 5000; // TCP 포트

    @Override
    public String handle() {
        Socket socket = null;
        BufferedReader reader = null;
        PrintWriter writer = null;
        String mqttMessage = "";

        try {
            // TCP 소켓 연결
            socket = new Socket(HOST, PORT);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            // 데이터 전송
            writer.println("Hello, Server!");

            // 서버로부터 데이터 읽기
            String line;
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("{\"deviceName\":\"tcp_device\", \"data\":[");

            while ((line = reader.readLine()) != null) {
                messageBuilder.append("{\"value\":\"").append(line).append("\"},");
            }

            // 마지막 콤마 제거 및 JSON 형식 닫기
            if (messageBuilder.charAt(messageBuilder.length() - 1) == ',') {
                messageBuilder.deleteCharAt(messageBuilder.length() - 1);
            }
            messageBuilder.append("]}");

            mqttMessage = messageBuilder.toString();
            log.debug("Generated MQTT Message: {}", mqttMessage);

        } catch (IOException e) {
            log.error("Error during TCP communication: {}", e.getMessage());
        } finally {
            // 자원 해제
            try {
                if (reader != null) reader.close();
                if (writer != null) writer.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                log.error("Error closing TCP resources: {}", e.getMessage());
            }
        }

        return mqttMessage;
    }
}
