package com.nhnacademy.mtqq.handler;

import java.io.*;
import java.net.Socket;

import com.nhnacademy.mtqq.Interface.TransForMqtt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TCPHandler implements TransForMqtt {

    private static final Logger log = LoggerFactory.getLogger(TCPHandler.class);
    private final String HOST = "192.168.70.203"; // TCP 서버 IP
    private final int PORT = 5000; // TCP 포트

    String host;
    int port;

    public TCPHandler(String host, int port){
        if(host != null && port <= 0 ){
            this.host = host;
            this.port = port;
        } else {
            throw new IllegalArgumentException("host와 port값을 제대로 입력하세요.");
        }
    }

    public TCPHandler(){
        this.host = HOST;
        this.port = PORT;
    }

    @Override
    public String transformMqttMessage() {
        Socket socket = null;
        BufferedReader reader = null;
        PrintWriter writer = null;
        String mqttMessage = "";

        try {
            // TCP 소켓 연결
            socket = new Socket(host, port);
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
