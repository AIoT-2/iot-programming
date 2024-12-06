package com.nhnacademy.modbus.impl;

import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class ModbusTcpClientImpl implements Runnable {

    private final String serverIp;

    private final int serverPort;

    public ModbusTcpClientImpl() {
        this("127.0.0.1", 8080);
    }

    public ModbusTcpClientImpl(String serverIp, int serverPort) {
        if (Objects.isNull(serverIp)) {
            log.warn("serverIp is Null!");
            throw new RuntimeException("serverIp is Null!");
        }
        if (serverPort < 1024 || 65535 < serverPort) {
            log.warn("serverPort range is Out!");
            throw new RuntimeException("serverPort range is Out!");
        }
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    @Override
    public void run() {

    }
}
