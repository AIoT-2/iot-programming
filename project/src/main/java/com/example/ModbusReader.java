package com.example;

import java.net.InetAddress;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;

public class ModbusReader {
    public static void main(String[] args) {
        // 로깅 설정
        Modbus.log().addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                System.out.println(record.getLevel().getName() + ": " + record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        });
        Modbus.setLogLevel(Modbus.LogLevel.LEVEL_DEBUG);

        try {
            // TCP 파라미터 설정
            TcpParameters tcpParameters = new TcpParameters();
            tcpParameters.setHost(InetAddress.getByName("192.168.70.203"));
            tcpParameters.setKeepAlive(true);
            tcpParameters.setPort(Modbus.TCP_PORT);

            // Modbus 마스터 생성
            ModbusMaster master = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
            Modbus.setAutoIncrementTransactionId(true);

            try {
                // 연결
                if (!master.isConnected()) {
                    master.connect();
                }

                // 데이터 읽기
                int slaveId = 1;
                int offset = 100;  // 채널 1 (캠퍼스)
                int quantity = 32;
                
                int[] registerValues = master.readInputRegisters(slaveId, offset, quantity);
                
                // 읽은 값 출력
                for (int i = 0; i < registerValues.length; i++) {
                    System.out.println("Address: " + (offset + i) + ", Value: " + registerValues[i]);
                }

                // 예시: 전압값 계산 (offset 16-17)
                int voltage = (registerValues[16] << 16) | registerValues[17];
                System.out.println("Voltage: " + (voltage / 100.0) + "V");

            } finally {
                master.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}