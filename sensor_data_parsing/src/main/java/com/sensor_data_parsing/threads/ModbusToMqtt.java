package com.sensor_data_parsing.threads;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.sensor_data_parsing.MqttClient;
import com.sensor_data_parsing.ValueMapper;
import com.sensor_data_parsing.interfaces.ProtocolToMqtt;

//Modbus로 데이터를 받고 포매팅을 한 후 MQTT로 보내는 역할을 수행하는 클래스.
public class ModbusToMqtt implements ProtocolToMqtt {
    private static final int DEFAULT_SLAVE_ID = 1; // 기본 Modbus 슬레이브 ID
    private static final int DEFAULT_START_ADDRESS = 100; // Modbus 통신에서의 기본 주소
    private static final int DEFAULT_QUANTITY = 32; // Modbus 통신에서의 기본 레지스터 수량
    private static final int DEFAULT_TRANSACTION_ID = 1; // Modbus 요청의 기본 트랜잭션 ID

    private static final ReadInputRegistersRequest request = new ReadInputRegistersRequest(); // Modbus 요청 객체
    private static final ObjectMapper objectMapper = new ObjectMapper(); // JSON 처리에 사용할 ObjectMapper 객체

    private final MqttClient publisher; // MQTT 클라이언트
    private final ModbusMaster m;

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
     * 지정된 MQTT 클라이언트와 TCP 파라미터를 사용하여 ModbusPublish 객체를 초기화합니다.
     *
     * @param publisher     MQTT 클라이언트 객체
     * @param tcpParameters TCP 파라미터 객체
     * @param slaveId       Modbus 슬레이브 ID
     * @param offset        시작 주소 오프셋
     * @param quantity      읽을 레지스터 수량
     * @param transactionId 트랜잭션 ID
     */
    public ModbusToMqtt(MqttClient publisher, TcpParameters tcpParameters, int slaveId, int startAddress, int quantity,
            int transactionId) {
        if (tcpParameters.getPort() != Modbus.TCP_PORT) {
            throw new IllegalArgumentException("Modbus포트가 아닙니다.");
        }
        this.publisher = publisher;

        try {
            request.setServerAddress(slaveId);
            request.setStartAddress(startAddress);
            request.setQuantity(quantity);
            request.setTransactionId(transactionId);
        } catch (ModbusNumberException e) {
            System.err.println("Modbus 요청 설정 중 오류 발생: " + e.getMessage());
        }

        this.m = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
        Modbus.setAutoIncrementTransactionId(true);
    }

    /**
     * 기본 설정을 사용하여 ModbusPublish 객체를 초기화합니다.
     *
     * @param publisher     MQTT 클라이언트 객체
     * @param tcpParameters TCP 파라미터 객체
     */
    public ModbusToMqtt(MqttClient publisher, TcpParameters tcpParameters) {
        this(publisher, tcpParameters, DEFAULT_SLAVE_ID, DEFAULT_START_ADDRESS, DEFAULT_QUANTITY,
                DEFAULT_TRANSACTION_ID);
    }

    @Override
    public String fetchDataFromProtocol() {
        ReadInputRegistersResponse response = fetchModbusData(m);

        int startAddress = request.getStartAddress();
        Map<String, Object> dataMap = getDataMap(response, startAddress);

        Map<String, Object> dataMessage = new HashMap<>();
        dataMessage.put("deviceName", channelMap.get(startAddress));
        dataMessage.put("data", dataMap);

        return convertToJson(dataMessage);
    }

    @Override
    public String[] convertToMqttFormat(String message) {
        return new String[] { "application/modbus", message };
    }

    @Override
    public void sendMessageToMqtt(String[] message) {
        publisher.sendMessage(message[0], message[1]);
    }

    /**
     * Modbus 데이터를 가져오는 별도의 메소드로 추출
     * 
     * @param m ModbusMaster 객체
     * @return ReadInputRegistersResponse 응답 객체
     * @throws ModbusProtocolException 프로토콜 오류
     * @throws ModbusIOException       IO 오류
     */
    private ReadInputRegistersResponse fetchModbusData(ModbusMaster m) {
        try {
            return (ReadInputRegistersResponse) m.processRequest(request);
        } catch (ModbusProtocolException e) {
            System.err.println("Modbus 프로토콜 오류: " + e.getMessage());
        } catch (ModbusIOException e) {
            System.err.println("Modbus IO 오류: " + e.getMessage());
        }

        return null; // 변환 실패 시 null 반환
    }

    /**
     * 30초 동안 대기하는 메소드
     */
    public void waitForNextCycle(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("스레드 대기 중 인터럽트 발생: " + e.getMessage());
        }
    }

    /**
     * 데이터를 JSON 문자열로 변환하는 메소드
     *
     * @param dataMessage 변환할 데이터
     * @return JSON 문자열
     */
    private String convertToJson(Map<String, Object> dataMessage) {
        try {
            return objectMapper.writeValueAsString(dataMessage);
        } catch (JsonProcessingException e) {
            System.err.println("메시지 JSON 변환 오류: " + e.getMessage());
            return null; // 변환 실패 시 null 반환
        }
    }

    /**
     * 레지스터 값을 읽고 처리하여 데이터 맵을 생성합니다.
     *
     * @param response     읽은 레지스터 응답
     * @param startAddress 시작 주소
     * @return 데이터 맵
     */
    private Map<String, Object> getDataMap(ReadInputRegistersResponse response, int startAddress) {
        Map<String, Object> dataMap = new HashMap<>();
        int offset = 0;

        while (offset < response.getHoldingRegisters().getQuantity()) {
            int registerValue;
            try {
                int address = startAddress + offset;

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
                System.err.println("주소 처리 오류: " + e.getMessage());
            }
        }

        return dataMap;
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

    /**
     * ModbusMaster 객체를 반환합니다.
     * 이 객체는 TCP 연결을 통해 Modbus 마스터와의 통신을 관리합니다.
     *
     * @return 이미 생성된 ModbusMaster 객체
     */
    public ModbusMaster getModbusMaster() {
        return m;
    }
}
