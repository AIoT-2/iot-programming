package com.nhnacademy.modbus;

import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;

import java.net.InetAddress;

public class ModbusDataProcessor {

    public static void main(String[] args) {
        try {
            TcpParameters tcpParameters = new TcpParameters();
            tcpParameters.setHost(InetAddress.getByName("192.168.70.203")); // 장치 IP 주소
            tcpParameters.setKeepAlive(true);
            tcpParameters.setPort(Modbus.TCP_PORT);

            ModbusMaster modbusMaster = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
            Modbus.setAutoIncrementTransactionId(true);

            int slaveId = 1;    // 슬레이브 ID
            int offset = 100;   // 시작 레지스터 주소
            int quantity = 4;   // 레지스터 개수 (32비트 값 2개)

            if (!modbusMaster.isConnected()) {
                modbusMaster.connect();
            }

            // 레지스터 값 읽기
            int[] registers = modbusMaster.readInputRegisters(slaveId, offset, quantity);

            // 16비트 레지스터 값을 32비트 정수로 변환
            int high = registers[0]; // 상위 16비트
            int low = registers[1];  // 하위 16비트
            int value = (high << 16) | (low & 0xFFFF); // 32비트 조합
            System.out.println("Raw Value (32-bit Integer): " + value);

            // 스케일링 및 단위 적용
            double scaledValue = value / 100.0; // 장치 문서에 따라 적절히 나눔
            System.out.println("Scaled Value: " + scaledValue + " °C");

            // 레지스터 값을 32비트 부동소수점 값으로 변환
            int floatHigh = registers[2]; // 상위 16비트
            int floatLow = registers[3];  // 하위 16비트
            int floatBits = (floatHigh << 16) | (floatLow & 0xFFFF); // 32비트 조합
            float floatValue = Float.intBitsToFloat(floatBits); // IEEE 754 float 변환
            System.out.println("Float Value: " + floatValue + " m/s");

        } catch (ModbusProtocolException | ModbusNumberException | ModbusIOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
