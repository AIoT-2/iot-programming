package com.nhnacademy.modbus;

import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.msg.request.ReadInputRegistersRequest;
import com.intelligt.modbus.jlibmodbus.msg.response.ReadInputRegistersResponse;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MasterTCP {
    private ModbusMaster modbusMaster;

    // Modbus 연결 설정
    public void connectModbusMaster(String host, int port) throws ModbusException, UnknownHostException {
        try {
            TcpParameters tcpParameters = new TcpParameters();
            tcpParameters.setHost(InetAddress.getByName(host));
            tcpParameters.setKeepAlive(true);
            tcpParameters.setPort(port);

            modbusMaster = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
            if (!modbusMaster.isConnected()) {
                modbusMaster.connect();
            }
        } catch (ModbusIOException e) {
            log.error("Failed to connect to Modbus server: {}", e.getMessage());
        }
    }

    // Modbus 데이터를 읽어 JSON 형식으로 변환하는 메서드
    public Map<String, Object> readModbusData(int slaveId, int offset, int quantity)
            throws ModbusIOException, ModbusProtocolException, ModbusNumberException {
        ReadInputRegistersRequest request = new ReadInputRegistersRequest();
        request.setServerAddress(slaveId);
        request.setStartAddress(offset);
        request.setQuantity(quantity);
        request.setTransactionId(1);

        ReadInputRegistersResponse response = (ReadInputRegistersResponse) modbusMaster.processRequest(request);

        // 읽어온 데이터를 맵 형태로 변환
        Map<String, Object> payloadData = new HashMap<>();
        payloadData.put("deviceId", slaveId);
        payloadData.put("timestamp", System.currentTimeMillis());

        Map<String, Object> measurements = new HashMap<>();
        for (int i = 0; i < response.getHoldingRegisters().getQuantity(); i++) {
            // 여기에 Modbus 주소 매핑을 통한 필드명 등을 추가할 수 있습니다.
            String fieldName = "register_" + i;
            int value = response.getHoldingRegisters().get(i);
            measurements.put(fieldName, value);
        }

        payloadData.put("measurements", measurements);
        return payloadData;
    }

    // Modbus 연결 종료
    public void disconnectModbusMaster() throws ModbusIOException {
        if (modbusMaster != null) {
            modbusMaster.disconnect();
        }
    }
}
