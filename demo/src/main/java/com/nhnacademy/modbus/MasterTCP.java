package com.nhnacademy.modbus;

import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.msg.request.ReadInputRegistersRequest;
import com.intelligt.modbus.jlibmodbus.msg.response.ReadInputRegistersResponse;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;
import com.nhnacademy.settings.DemoSetting;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MasterTCP {
    private ModbusMaster modbusMaster;

    public MasterTCP() {
        try {
            // IP와 포트 번호를 제공하여 ModbusMasterTCP 객체 생성
            TcpParameters tcpParameters = new TcpParameters();
            tcpParameters.setHost(InetAddress.getByName(DemoSetting.MODBUS_HOST)); // 예시 IP
            tcpParameters.setPort(Modbus.TCP_PORT); // 예시 포트

            modbusMaster = ModbusMasterFactory.createModbusMasterTCP(tcpParameters); // 올바른 생성자 사용
            modbusMaster.connect(); // 연결 시도
            log.info("ModbusMaster connected successfully.");
        } catch (Exception e) {
            log.error("Error initializing ModbusMasterTCP: " + e.getMessage());
        }
    }

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

        Map<Integer, String[]> registerMap = ConfigurationData.addressMapName();

        for (int i = 0; i < response.getHoldingRegisters().getQuantity(); i++) {
            int address = offset + i; // 실제 레지스터 주소

            // 레지스터 주소에 대한 정보가 있는지 확인
            String[] registerInfo = registerMap.get(address);
            if (registerInfo != null) {
                String fieldName = registerInfo[0]; // 필드의 이름
                int combineRegister = (response.getHoldingRegisters().get(i) << 16)
                        | response.getHoldingRegisters().get(i + 1);
                int value; // Modbus로 읽어온 값

                if (registerInfo[2].equals("32")) {
                    value = combineRegister;
                } else {
                    value = response.getHoldingRegisters().get(i);
                }

                if (value != 0) {
                    measurements.put(fieldName, ConfigurationData.applyScale(i, value));
                }
            }
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
