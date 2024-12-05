package com.sensor_data_parsing;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

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

public class ModbusPublish {
    private static final Map<Integer, ValueMapper> addressMap = new HashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
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

    private static final Map<Integer, String> channelMap = new HashMap<>();
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

    // 메시지를 보낼 브로커
    private static final String DEFAULT_MQTT_IDENTIFIER = "mqttClient";
    private static final String DEFAULT_MQTT_HOST = "localhost";
    private static final int DEFAULT_MQTT_PORT = 8888;
    private static final String DEFAULT_MQTT_USER_NAME = "";
    private static final String DEFAULT_MQTT_PASSWORD = "";
    private static final String DEFAULT_IP_ADDRESS = "192.168.70.203";
    private static final int DEFAULT_PORT = Modbus.TCP_PORT;
    private static final boolean DEFAULT_SET_KEEP_ALIVE = true;
    private static final int DEFAULT_SLAVE_ID = 1;
    private static final int DEFAULT_START_ADDRESS = 100;
    private static final int DEFAULT_OFFSET = 0;
    private static final int DEFAULT_QUANTITY = 32;
    private static final int DEFAULT_TRANSACTION_ID = 1;

    public static void main(String[] args) {
        MqttClient mqttClient = new MqttClient(DEFAULT_MQTT_IDENTIFIER, DEFAULT_MQTT_HOST, DEFAULT_MQTT_PORT);
        mqttClient.connectToMqtt(DEFAULT_MQTT_USER_NAME, DEFAULT_MQTT_PASSWORD);

        Modbus.setLogLevel(Modbus.LogLevel.LEVEL_DEBUG);

        TcpParameters tcpParameters = connetTcp(DEFAULT_IP_ADDRESS, DEFAULT_PORT, DEFAULT_SET_KEEP_ALIVE);

        ModbusMaster m = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
        Modbus.setAutoIncrementTransactionId(true);

        if (!m.isConnected()) {
            try {
                m.connect();
            } catch (ModbusIOException ex) {
            }
        }

        String newTopic = "application/modbus";

        ReadInputRegistersRequest request = request(DEFAULT_SLAVE_ID, DEFAULT_START_ADDRESS,
                DEFAULT_QUANTITY, DEFAULT_TRANSACTION_ID);
        ReadInputRegistersResponse response = null;
        try {
            response = (ReadInputRegistersResponse) m.processRequest(request);
        } catch (ModbusProtocolException | ModbusIOException e) {
            e.printStackTrace();
        }

        int startAddress = request.getStartAddress();
        Map<String, Object> dataMap;
        dataMap = getDataMap(response, startAddress);

        Map<String, Object> dataMessage = new HashMap<>();
        dataMessage.put("deviceName", channelMap.get(startAddress));
        dataMessage.put("data", dataMap);
        String message = null;
        try {
            message = objectMapper.writeValueAsString(dataMessage);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        // 메시지를 보낼 새 MQTT 브로커로 메시지 발행
        mqttClient.sendMessage(newTopic, message);

        try {
            m.disconnect();
        } catch (ModbusIOException e) {
            e.printStackTrace();
        }
    }

    // 레지스터 두개를 합쳐서 32비트를 사용
    public static int registerToInt32(int firstRegisterValue, int secondRegisterValue) {
        return (firstRegisterValue << 16)
                | secondRegisterValue;
    }

    // double타입이 int타입으로 바꿔도 되는지 확인
    public static boolean checkDoubleType(double registerValueDouble) {
        return registerValueDouble == Math.floor(registerValueDouble);
    }

    public static ReadInputRegistersRequest request(int slaveId, int offset, int quantity, int transactionId) {
        ReadInputRegistersRequest request = new ReadInputRegistersRequest();
        try {
            request.setServerAddress(slaveId);
            request.setStartAddress(offset);
            request.setQuantity(quantity);
            request.setTransactionId(transactionId);
        } catch (ModbusNumberException e) {
            e.printStackTrace();
        }

        return request;
    }

    // TCP 연결
    public static TcpParameters connetTcp(String ipAddress, int port, boolean setKeepAlive) {
        TcpParameters tcpParameters = new TcpParameters();
        try {
            // 연결할 서버의 IP 주소 설정
            tcpParameters.setHost(InetAddress.getByName(ipAddress));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        tcpParameters.setPort(port);
        tcpParameters.setKeepAlive(setKeepAlive);

        return tcpParameters;
    }

    // 레지스터 값을 읽고 처리
    public static Map<String, Object> getDataMap(ReadInputRegistersResponse response, int startAddress) {

        Map<String, Object> dataMap = new HashMap<>();
        for (int offset = 0; offset < response.getHoldingRegisters().getQuantity(); offset++) {
            int registerValue;
            try {
                registerValue = response.getHoldingRegisters().get(offset);

                int address = startAddress + offset;

                String addressName = addressMap.get(offset).getName();
                double scale = addressMap.get(offset).getScale();

                if (addressMap.get(offset).getIs32bit()) {
                    int firstRegisterValue = response.getHoldingRegisters().get(offset);
                    int secondRegisterValue = response.getHoldingRegisters().get(offset + 1);

                    registerValue = registerToInt32(firstRegisterValue, secondRegisterValue);
                    offset++; // 두 개의 레지스터를 처리했으므로 offset을 1 증가시킴
                }

                double registerValueDouble = registerValue / scale;

                System.out.println("Address: " + address + ", Value: " + registerValue);
                // registerValueDouble이 정수와 같으면 int로 변환
                dataMap.put(addressName,
                        checkDoubleType(registerValueDouble) ? (int) registerValueDouble
                                : (double) registerValueDouble);
            } catch (IllegalDataAddressException e) {
                e.printStackTrace();
            }
        }

        return dataMap;
    }
}
