package com.nhnacademy.modbus;

import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;

import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;

public class ContinuousSensorReader {

    public static void main(String[] args) {
        try {
            // TCP 설정
            TcpParameters tcpParameters = new TcpParameters();
            tcpParameters.setHost(InetAddress.getByName("192.168.70.203")); // 슬레이브 디바이스 IP
            tcpParameters.setKeepAlive(true);
            tcpParameters.setPort(Modbus.TCP_PORT);

            // Modbus 마스터 생성
            ModbusMaster modbusMaster = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
            Modbus.setAutoIncrementTransactionId(true);

            int slaveId = 1;    // 슬레이브 ID
            int offset = 100;   // 레지스터 시작 주소
            int quantity = 32;  // 읽을 레지스터 개수

            // 지속적인 데이터 수집을 위한 타이머 설정
            Timer timer = new Timer(true);
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
                        if (!modbusMaster.isConnected()) {
                            modbusMaster.connect();
                        }

                        // 센서 데이터 읽기
                        int[] registerValues = modbusMaster.readInputRegisters(slaveId, offset, quantity);

                        // 읽어온 데이터 출력
                        System.out.println("=== Sensor Data ===");
                        for (int i = 0; i < registerValues.length; i++) {
                            System.out.println("Address: " + (offset + i) + ", Value: " + registerValues[i]);
                        }

                        // 특정 레지스터 조합 (예: 16, 17번 레지스터)
                        int value = (registerValues[16] << 16) | registerValues[17];
                        System.out.println("Combined Value (V1): " + (value / 100.0));

                    } catch (ModbusProtocolException | ModbusNumberException | ModbusIOException e) {
                        System.err.println("Error reading data: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }, 0, 5000); // 5초 간격으로 실행

            System.out.println("Sensor data collection started. Press Ctrl+C to stop.");

            // 메인 스레드가 종료되지 않도록 대기
            Thread.sleep(Long.MAX_VALUE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
