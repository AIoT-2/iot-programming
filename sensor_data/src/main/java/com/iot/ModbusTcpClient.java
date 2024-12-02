package com.iot;

import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersRequest;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersResponse;

public class ModbusTcpClient {
    public static void main(String[] args) {
        // 1. 서버 IP 및 포트 설정
        String setHost = "192.168.70.203"; // 서버 IP
        int setPort = 502; // 서버 포트
        int slaveId = 1; // 슬레이브 ID
        int startOffset = 0; // 읽기 시작 주소
        int numberOfRegisters = 10; // 읽을 레지스터 수

        IpParameters ipParameters = new IpParameters();
        ipParameters.setHost(setHost);
        ipParameters.setPort(setPort);

        // 2. Modbus TCP Master 설정
        ModbusFactory modbusFactory = new ModbusFactory();
        ModbusMaster master = modbusFactory.createTcpMaster(ipParameters, true);

        try {
            // 3. Modbus 연결
            master.init();
            System.out.println("Connected to Modbus server");

            // Create the Read Holding Registers request
            ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(slaveId, startOffset,
                    numberOfRegisters);

            // Send the request and get the response
            ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) master.send(request);

            // Process the response
            if (response != null && response.isException()) {
                System.out.println("Modbus data from Modbus server: ");
                for (int i = 0; i < numberOfRegisters; i++) {
                    System.out.printf("Register %d: %d\n", startOffset + i, response.getShortData()[i]);
                }
            } else {
                System.out.println("Failed to receive valid response or exception occurred.");
            }

        } catch (ModbusInitException e) {
            System.err.println("Failed to initialize Modbus Master: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error in Modbus communication: " + e.getMessage());
        } finally {
            // 7. Destroy the master to close the connection
            master.destroy();
            System.out.println("Disconnected from Modbus server.");
        }
    }
}