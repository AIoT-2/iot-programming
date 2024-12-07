package com.sensor_data_flow.client;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataAddressException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.msg.request.ReadInputRegistersRequest;
import com.intelligt.modbus.jlibmodbus.msg.response.ReadInputRegistersResponse;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;
import com.sensor_data_flow.ValueMapper;

/**
 * Modbus TCP 통신을 통해 데이터를 읽고 처리하는 기능을 제공합니다.
 * 이 클래스는 ModbusMaster 객체를 사용하여 Modbus 서버와 TCP로 통신하며,
 * 읽은 데이터를 특정 포맷으로 변환하여 반환합니다.
 */
public class ModbusTcpClient {
    static final Logger logger = LoggerFactory.getLogger(ModbusTcpClient.class);

    private static final ReadInputRegistersRequest request = new ReadInputRegistersRequest(); // Modbus 요청 객체

    private final ModbusMaster m; // Modbus 통신을 담당하는 객체

    private static final Map<Integer, ValueMapper> addressMap = new HashMap<>(); // 레지스터 정보를 저장하는 맵
    static {
        addressMap.put(0, new ValueMapper("type"));
        addressMap.put(1, new ValueMapper("a leakage current"));
        addressMap.put(2, new ValueMapper("current", 100, true));
        addressMap.put(4, new ValueMapper("W", true));
        addressMap.put(6, new ValueMapper("VAR", true));
        addressMap.put(8, new ValueMapper("VA", true));
        addressMap.put(10, new ValueMapper("PF average", 100));
        addressMap.put(11, new ValueMapper("reserved"));
        addressMap.put(12, new ValueMapper("current unbalance", 100));
        addressMap.put(13, new ValueMapper("I THD average", 100));
        addressMap.put(14, new ValueMapper("IGR", 10));
        addressMap.put(15, new ValueMapper("IGC", 10));
        addressMap.put(16, new ValueMapper("V1", 100, true));
        addressMap.put(18, new ValueMapper("I1", 100, true));
        addressMap.put(20, new ValueMapper("W", true));
        addressMap.put(22, new ValueMapper("VAR", true));
        addressMap.put(24, new ValueMapper("VA", true));
        addressMap.put(26, new ValueMapper("volt unbalance", 100));
        addressMap.put(27, new ValueMapper("current unbalance", 100));
        addressMap.put(28, new ValueMapper("phase", 100));
        addressMap.put(29, new ValueMapper("power factor", 100));
        addressMap.put(30, new ValueMapper("I1 THD", 100));
        addressMap.put(31, new ValueMapper("reserved"));
        addressMap.put(32, new ValueMapper("V2", 100, true));
    }

    private static final Map<Integer, String> channelMap = new HashMap<>(); // 채널별 정보를 저장하는 맵
    static {
        channelMap.put(100, "캠퍼스");
        channelMap.put(200, "캠퍼스1");
        channelMap.put(300, "전등1");
        channelMap.put(400, "전등2");
        channelMap.put(500, "전등3");
        channelMap.put(600, "강의실b_바닥");
        channelMap.put(700, "강의실a_바닥1");
        channelMap.put(800, "강의실a_바닥2");
        channelMap.put(900, "프로젝터1");
        channelMap.put(1000, "프로젝터2");
        channelMap.put(1100, "회의실");
        channelMap.put(1200, "서버실");
        channelMap.put(1300, "간판");
        channelMap.put(1400, "페어룸");
        channelMap.put(1500, "사무실_전열1");
        channelMap.put(1600, "사무실_전열2");
        channelMap.put(1700, "사무실_복사기");
        channelMap.put(1800, "빌트인_전열");
        channelMap.put(1900, "사무실_전열3");
        channelMap.put(2000, "정수기");
        channelMap.put(2100, "하이브_전열");
        channelMap.put(2200, "바텐_전열");
        channelMap.put(2300, "S_P");
        channelMap.put(2400, "공조기");
        channelMap.put(2500, "AC");
    }

    /**
     * ModbusTcpClient 클래스의 생성자
     * Modbus TCP 요청 객체의 파라미터를 설정합니다.
     *
     * @param tcpParameters Modbus 서버의 TCP 파라미터 (IP 주소, 포트 등)
     */
    public ModbusTcpClient(TcpParameters tcpParameters) {
        // 포트 번호가 Modbus TCP 기본 포트가 아닌 경우 예외 발생
        if (tcpParameters.getPort() != Modbus.TCP_PORT) {
            throw new IllegalArgumentException("Modbus포트가 아닙니다.");
        }

        this.m = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
        Modbus.setAutoIncrementTransactionId(true);
    }

    /**
     * Modbus 요청 객체의 설정을 관리
     * 
     * @param slaveId       Modbus 슬레이브 ID
     * @param startAddress  레지스터 시작 주소
     * @param quantity      읽을 레지스터의 수
     * @param transactionId 트랜잭션 ID
     */
    public void setModbusRequest(int slaveId, int startAddress, int quantity, int transactionId) {
        try {
            request.setServerAddress(slaveId);
            request.setStartAddress(startAddress);
            request.setQuantity(quantity);
            request.setTransactionId(transactionId);
        } catch (ModbusNumberException e) {
            logger.error("Modbus 요청 설정 중 오류 발생: {}", e.getMessage()); // 에러 메시지를 로그로 기록
        }
    }

    /**
     * Modbus 데이터를 가져오는 메소드
     * ModbusMaster 객체를 통해 데이터를 요청하고 응답을 반환합니다.
     *
     * @return Modbus 데이터 응답 객체 (ReadInputRegistersResponse)
     * @throws ModbusProtocolException 프로토콜 오류
     * @throws ModbusIOException       IO 오류
     */
    public ReadInputRegistersResponse fetchModbusData() throws ModbusProtocolException, ModbusIOException {
        return (ReadInputRegistersResponse) m.processRequest(request);
    }

    /**
     * 레지스터 값을 읽고 처리하여 데이터 맵을 생성합니다.
     *
     * @param response 읽은 레지스터 응답
     * @return 데이터 맵
     */
    public Map<String, Object> getDataMap(ReadInputRegistersResponse response) {
        if (response == null) {
            throw new IllegalArgumentException("response가 null이므로 레지스터 값을 읽을수가 없습니다.");
        }

        Map<String, Object> dataMap = new HashMap<>();
        int offset = 0;

        while (offset < response.getHoldingRegisters().getQuantity()) {
            int registerValue;
            try {
                int address = getStartAddress() + offset;

                String addressName = addressMap.get(offset).getName();
                double scale = addressMap.get(offset).getScale();

                if (addressMap.get(offset).getIs32bit()) {
                    int firstRegisterValue = response.getHoldingRegisters().get(offset);
                    int secondRegisterValue = response.getHoldingRegisters().get(offset + 1);

                    registerValue = registerToInt32(firstRegisterValue, secondRegisterValue);
                    offset += 2; // 두 개의 레지스터를 처리했으므로 offset을 2 증가시킴
                } else {
                    registerValue = response.getHoldingRegisters().get(offset);
                    offset++;
                }

                double registerValueDouble = registerValue / scale;

                System.out.println("Address: " + address + ", Value: " + registerValue);
                // registerValueDouble이 정수와 같으면 int로 변환
                dataMap.put(addressName,
                        checkDoubleType(registerValueDouble) ? (int) registerValueDouble
                                : (double) registerValueDouble);
            } catch (IllegalDataAddressException e) {
                logger.error("주소 처리 오류: {}", e.getMessage());
            }
        }

        return dataMap;
    }

    /**
     * ModbusMaster 객체를 반환합니다.
     * 이 객체는 TCP 연결을 통해 Modbus 마스터와의 통신을 관리합니다.
     *
     * @return 이미 생성된 ModbusMaster 객체
     */
    public ModbusMaster getModbusMaster() {
        return m;
    }

    /**
     * 현재 채널 위치에 대한 이름을 반환하는 메소드
     * 
     * @return 채널 이름 (예: "캠퍼스", "전등1" 등)
     */
    public String getChannelLocation() {
        return channelMap.get(getStartAddress());
    }

    /**
     * 두 개의 레지스터 값을 합쳐서 32비트 정수로 변환합니다.
     *
     * @param firstRegisterValue  첫 번째 레지스터 값
     * @param secondRegisterValue 두 번째 레지스터 값
     * @return 결합된 32비트 정수 값
     */
    private int registerToInt32(int firstRegisterValue, int secondRegisterValue) {
        return (firstRegisterValue << 16)
                | secondRegisterValue;
    }

    /**
     * 주어진 값이 정수인지 확인합니다.
     *
     * @param value 검사할 값
     * @return 정수 여부
     */
    private boolean checkDoubleType(double registerValueDouble) {
        return registerValueDouble == Math.floor(registerValueDouble);
    }

    private int getStartAddress() {
        return request.getStartAddress();
    }
}
