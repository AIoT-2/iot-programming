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
            tcpParameters.setHost(InetAddress.getByName(DemoSetting.MODBUS_HOST));
            tcpParameters.setPort(Modbus.TCP_PORT);

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
            throws ModbusIOException, ModbusProtocolException, ModbusNumberException, UnknownHostException,
            ModbusException {
        if (modbusMaster == null || !modbusMaster.isConnected()) {
            connectModbusMaster(DemoSetting.MODBUS_HOST, Modbus.TCP_PORT);
        }

        ReadInputRegistersRequest request = new ReadInputRegistersRequest();
        request.setServerAddress(slaveId);
        request.setStartAddress(offset);
        request.setQuantity(quantity);
        request.setTransactionId(1);

        ReadInputRegistersResponse response = (ReadInputRegistersResponse) modbusMaster.processRequest(request);
        Map<Integer, String> nameMap = ConfigurationData.topicMapName();
        String topic = nameMap.get(offset);
        String name = topic.split("/")[2];

        // 읽어온 데이터를 맵 형태로 변환
        Map<String, Object> payloadData = new HashMap<>();
        payloadData.put("deviceId", slaveId);
        payloadData.put("timestamp", System.currentTimeMillis());
        payloadData.put("topic", topic);

        Map<String, Object> measurements = new HashMap<>();

        Map<Integer, String[]> registerMap = ConfigurationData.addressMapName();

        int i = 0; // 인덱스 초기화
        while (i < response.getHoldingRegisters().getQuantity()) {
            // 레지스터 주소에 대한 정보가 있는지 확인
            String[] registerInfo = registerMap.get(i);
            if (registerInfo != null) {
                String fieldName = registerInfo[0]; // 필드 이름
                String dataSize = registerInfo[2]; // 데이터 크기(16비트 또는 32비트)

                // 16비트나 32비트 조합으로 값을 결합
                int value = 0;
                if ("32".equals(dataSize)) {
                    // 32비트로 결합된 값 처리
                    int combineRegister = (response.getHoldingRegisters().get(i) << 16)
                            | response.getHoldingRegisters().get(i + 1);
                    value = combineRegister;
                    if (value != 0) {
                        measurements.put(fieldName, ConfigurationData.applyScale(i, value));
                    }
                    i += 2; // 32비트이므로 두 레지스터를 처리했으므로 i를 2만큼 증가시킴
                } else {
                    // 16비트 값 처리
                    value = response.getHoldingRegisters().get(i);
                    if (value != 0) {
                        // 값에 스케일을 적용하고 measurements에 넣음
                        measurements.put(fieldName, ConfigurationData.applyScale(i, value));
                    }
                    i++; // 16비트일 경우 i를 1만큼 증가시킴
                }
            } else {
                i++;
            }
            measurements.put("name", name);
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
