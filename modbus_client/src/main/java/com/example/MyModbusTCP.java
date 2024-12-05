package com.example;

import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.msg.request.ReadInputRegistersRequest;
import com.intelligt.modbus.jlibmodbus.msg.response.ReadInputRegistersResponse;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;

import java.net.InetAddress;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

// TODO 1 : print -> log로 변경
// TODO 2 : 클래스 분리
// TODO 3 : Modbus temperature 주소 불러오기 map이나 JSON 이용

public class MyModbusTCP {
    static public void main(String[] args) {
        Modbus.log().addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                System.out.println(record.getLevel().getName() + ": " + record.getMessage());
            }

            @Override
            public void flush() {
                // do nothing
            }

            @Override
            public void close() throws SecurityException {
                // do nothing
            }
        });
        Modbus.setLogLevel(Modbus.LogLevel.LEVEL_DEBUG);

        try {
            TcpParameters tcpParameters = new TcpParameters();
            tcpParameters.setHost(InetAddress.getByName("192.168.70.203"));
            tcpParameters.setKeepAlive(true);
            tcpParameters.setPort(Modbus.TCP_PORT);

            ModbusMaster m = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
            Modbus.setAutoIncrementTransactionId(true);

            int slaveId = 1; // a slave address
            int startAddress = 100; // starting register address
            int temperatureCount = 10; // the number of registers
            double scale = 10.0; // 스케일 (값을 나누는 기준)

            try {
                if (!m.isConnected()) {
                    m.connect();
                }

                // 온도 데이터 읽기 (레지스터 개수 = 온도 값 개수 × 1)
                int[] temperatureData = m.readInputRegisters(slaveId, startAddress, temperatureCount);

                // 레지스터값 출력
                System.out.println("Register Values:");
                for (int i = 0; i < temperatureData.length; i++) {
                    System.out.printf("Address %d: %d%n", startAddress + i, temperatureData[i]);
                }

                // 각 온도 값 계산
                for (int i = 0; i < temperatureData.length; i++) {
                    double temperature = temperatureData[i] / scale;
                    System.out.println("Temperature " + (i + 1) + ": " + temperature + " °C");
                }

            } catch (ModbusProtocolException | ModbusNumberException | ModbusIOException e) {
                e.printStackTrace();
            } finally {
                try {
                    m.disconnect();
                } catch (ModbusIOException e) {
                    e.printStackTrace();
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}