package com.example;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class ModbusTcpClient {
    public static void main(String[] args) {
        String serverIp = "192.168.70.203"; // 서버 IP
        int serverPort = 502; // 서버 포트

        try (Socket socket = new Socket(serverIp, serverPort)) {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            // 1. MODBUS 요청 생성 (Read Holding Registers)
            byte[] request = new byte[] {
                    0x00, 0x01, // Transaction Identifier
                    0x00, 0x00, // Protocol Identifier
                    0x00, 0x06, // Message Length
                    0x01, // Unit Identifier (슬레이브 ID)
                    0x03, // Function Code (Read Holding Registers)
                    0x00, 0x00, // Start Address (0)
                    0x00, 0x0A // Quantity (10 registers)
            };
            out.write(request);
            System.out.println("Request sent to server");

            // 2. 서버 응답 읽기
            byte[] response = new byte[256];
            int bytesRead = in.read(response);
            System.out.println("Response received, length: " + bytesRead);

            // 3. 응답 데이터 출력
            for (int i = 0; i < bytesRead; i++) {
                System.out.printf("Byte %d: 0x%02X\n", i, response[i]);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
