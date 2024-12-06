package com.example;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;


public class ModbusToMqttBridge {
    private static final Logger logger = Logger.getLogger(ModbusToMqttBridge.class.getName());
    private final ModbusMaster master;
    private final Map<Integer, String> channelLocations;
    private final Gson gson;
    private final MqttPublisher mqttPublisher;

    public ModbusToMqttBridge(String modbusHost, int modbusPort, String mqttBroker) throws Exception {
        // Modbus 설정
        TcpParameters tcpParameters = new TcpParameters();
        tcpParameters.setHost(InetAddress.getByName(modbusHost));
        tcpParameters.setKeepAlive(true);
        tcpParameters.setPort(modbusPort);

        this.master = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
        Modbus.setAutoIncrementTransactionId(true);

        this.channelLocations = initializeChannelLocations();
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        // MQTT Publisher 초기화
        this.mqttPublisher = new MqttPublisher(mqttBroker, "ModbusConverter", "power");
    }

    private Map<Integer, String> initializeChannelLocations() {
        Map<Integer, String> locations = new HashMap<>();
        locations.put(1, "캠퍼스");
        locations.put(2, "캠퍼스1");
        locations.put(3, "전등1");
        locations.put(4, "전등2");
        locations.put(5, "전등3");
        locations.put(6, "강의실b_바닥");
        locations.put(7, "강의실a_바닥1");
        locations.put(8, "강의실a_바닥2");
        locations.put(9, "프로젝터1");
        locations.put(10, "프로젝터2");
        locations.put(11, "회의실");
        locations.put(12, "서버실");
        locations.put(13, "간판");
        locations.put(14, "페어룸");
        locations.put(15, "사무실_전열1");
        locations.put(16, "사무실_전열2");
        locations.put(17, "사무실_복사기");
        locations.put(18, "빌트인_전열");
        locations.put(19, "사무실_전열3");
        locations.put(20, "정수기");
        locations.put(21, "하이브_전열");
        locations.put(22, "바텐_전열");
        locations.put(23, "S_P");
        locations.put(24, "공조기");
        return locations;
    }

    private String convertToJson(int[] registerValues, String location) {
        Map<String, Object> data = new HashMap<>();

        // 기본 정보
        data.put("location", location);
        data.put("timestamp", System.currentTimeMillis());

        // type (offset 0)
        int type = registerValues[0];
        data.put("deviceType", getDeviceType(type));

        // current (offset 2-3) - UINT32, scale 100
        long current = combineRegisters(registerValues[2], registerValues[3]);
        data.put("current", current / 100.0);

        // power W (offset 4-5) - INT32, scale 1
        int powerW = combineRegistersToInt(registerValues[4], registerValues[5]);
        data.put("powerW", powerW);

        // VAR (offset 6-7) - INT32, scale 1
        int var = combineRegistersToInt(registerValues[6], registerValues[7]);
        data.put("powerVAR", var);

        // VA (offset 8-9) - UINT32, scale 1
        long va = combineRegisters(registerValues[8], registerValues[9]);
        data.put("powerVA", va);

        // PF average (offset 10) - INT16, scale 100
        data.put("powerFactor", registerValues[10] / 100.0);

        // current unbalance (offset 12) - UINT16, scale 100
        data.put("currentUnbalance", registerValues[12] / 10000.0);

        // I THD average (offset 13) - UINT16, scale 100
        data.put("currentTHD", registerValues[13] / 10000.0);

        // Voltage (offset 16-17) - UINT32, scale 100
        long voltage = combineRegisters(registerValues[16], registerValues[17]);
        data.put("voltage", voltage / 100.0);

        return gson.toJson(data);
    }

    private String getDeviceType(int type) {
        switch (type) {
            case 0:
                return "NOT_USED";
            case 1:
                return "1P_R";
            case 2:
                return "1P_S";
            case 3:
                return "1P_T";
            case 4:
                return "3P3W_2CT";
            case 5:
                return "3P4W";
            case 6:
                return "ZCT";
            case 7:
                return "3P3W_3CT";
            case 8:
                return "1P3W";
            case 9:
                return "ZCT_A";
            case 10:
                return "ZCT_B";
            case 11:
                return "ZCT_C";
            default:
                return "UNKNOWN";
        }
    }

    private long combineRegisters(int reg1, int reg2) {
        return ((long) (reg1 & 0xFFFF) << 16) | (reg2 & 0xFFFF);
    }

    private int combineRegistersToInt(int reg1, int reg2) {
        return (reg1 << 16) | (reg2 & 0xFFFF);
    }

private String determineCategory(String location) {
    if (location.contains("사무실")) return "office";
    if (location.contains("서버")) return "server";
    if (location.contains("강의실")) return "classroom";
    if (location.contains("전등")) return "lighting";
    if (location.contains("프로젝터")) return "projector";
    if (location.contains("하이브")) return "hive";
    if (location.contains("바텐")) return "cafe";
    return "others";
}

public void processChannel(int channelNumber) throws Exception {
    String location = channelLocations.get(channelNumber);
    if (location == null) {
        logger.warning("Unknown channel number: " + channelNumber);
        return;
    }

    logger.info("Processing channel " + channelNumber + " (" + location + ")");

    try {
        if (!master.isConnected()) {
            master.connect();
        }

        int slaveId = 1;
        int offset = channelNumber * 100;
        int quantity = 32;

        // Modbus 데이터 읽기
        int[] registerValues = master.readInputRegisters(slaveId, offset, quantity);

        // JSON 변환
        String jsonData = convertToJson(registerValues, location);

        // 카테고리 결정
        String category = determineCategory(location);
        
        // 새로운 토픽 구조로 발행
        mqttPublisher.publish("devices/" + category + "/" + location, jsonData);

        // 콘솔에 출력
        logger.info("Channel " + channelNumber + " (" + category + "/" + location + ") JSON data:");
        logger.info(jsonData);

    } catch (Exception e) {
        logger.warning("Error reading channel " + channelNumber + ": " + e.getMessage());
        throw e;
    }
}

public void processAllChannels() {
    try {
        Map<String, Map<String, Object>> groupedData = new HashMap<>();

        // 모든 채널의 데이터 수집 및 그룹화
        for (int channelNumber : channelLocations.keySet()) {
            try {
                String location = channelLocations.get(channelNumber);
                String category = determineCategory(location);

                if (!master.isConnected()) {
                    master.connect();
                }

                int[] registerValues = master.readInputRegisters(1, channelNumber * 100, 32);
                Map<String, Object> deviceData = new HashMap<>();

                // 데이터 변환
                deviceData.put("deviceType", getDeviceType(registerValues[0]));
                deviceData.put("current", combineRegisters(registerValues[2], registerValues[3]) / 100.0);
                deviceData.put("powerW", combineRegistersToInt(registerValues[4], registerValues[5]));
                deviceData.put("powerVAR", combineRegistersToInt(registerValues[6], registerValues[7]));
                deviceData.put("powerVA", combineRegisters(registerValues[8], registerValues[9]));
                deviceData.put("powerFactor", registerValues[10] / 100.0);
                deviceData.put("currentUnbalance", registerValues[12] / 10000.0);
                deviceData.put("currentTHD", registerValues[13] / 10000.0);
                deviceData.put("voltage", combineRegisters(registerValues[16], registerValues[17]) / 100.0);
                deviceData.put("timestamp", System.currentTimeMillis());
                
                // 그룹화된 데이터 저장
                if (!groupedData.containsKey(category)) {
                    groupedData.put(category, new HashMap<>());
                }
                groupedData.get(category).put(location, deviceData);

            } catch (Exception e) {
                logger.warning("Error processing channel " + channelNumber + ": " + e.getMessage());
            }
        }

        // 그룹화된 데이터 발행
        for (Map.Entry<String, Map<String, Object>> group : groupedData.entrySet()) {
            String category = group.getKey();
            String jsonData = gson.toJson(group.getValue());
            mqttPublisher.publish("devices/" + category, jsonData);
            logger.info("Published grouped data for category: " + category);
        }

    } catch (Exception e) {
        logger.warning("Error in processing all channels: " + e.getMessage());
    }
}

    public void close() throws Exception {
        if (master != null && master.isConnected()) {
            master.disconnect();
        }
        if (mqttPublisher != null) {
            mqttPublisher.close();
        }
    }

    public static void main(String[] args) {
        ModbusToMqttBridge bridge = null;
        try {
            bridge = new ModbusToMqttBridge(
                    "192.168.70.203", // Modbus 호스트
                    502, // Modbus 포트
                    "tcp://localhost:1883" // 최종 브로커 주소
            );

            while (true) {
                try {
                    bridge.processAllChannels();
                    Thread.sleep(10000); // 10초 대기
                } catch (Exception e) {
                    logger.warning("Error in processing cycle: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.severe("Critical error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (bridge != null) {
                try {
                    bridge.close();
                } catch (Exception e) {
                    logger.severe("Error while closing bridge: " + e.getMessage());
                }
            }
        }
    }
}