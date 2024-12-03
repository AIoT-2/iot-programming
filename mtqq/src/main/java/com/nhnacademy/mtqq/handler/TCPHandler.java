package com.nhnacademy.mtqq.handler;

import com.nhnacademy.mtqq.Interface.DataSourceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class TCPHandler implements DataSourceHandler {
    private static final Logger log = LoggerFactory.getLogger(TCPHandler.class);

    @Override
    public String transformToMqtt() {
        try(Socket socket = new Socket("192.168.70.203", 502);
            DataInputStream in = new DataInputStream(socket.getInputStream())) {

            byte[] buffer = new byte[256];
            int bytesRead =  in.read(buffer);

            StringBuilder data = new StringBuilder("{ \"source\": \"TCP\", \"data\": \"");
            for(int i = 0; i < bytesRead; i++){
                data.append(String.format("0x%02X ", buffer[i]));
            }
            data.append("\" }");

            return data.toString();

        } catch (IOException e) {
            log.debug("Exception: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public boolean canHandle(String sourceType) {
        return "TCP".equalsIgnoreCase(sourceType);
    }
}
