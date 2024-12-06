package com.nhnacademy.mtqq.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;
import com.nhnacademy.mtqq.Interface.TransForMqtt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModbusHandler implements TransForMqtt {
    private static final Logger log = LoggerFactory.getLogger(ModbusHandler.class);

    private String host;
    private int port;
    private int slaveId;
    private int offset;
    private int quantity;
    private String addressFilePath;
    private String channelFilePath;
    private String offsetFilePath;

    private static final String ADDRESS_PATH = "addresses.json";
    private static final String CHANNEL_PATH = "channels.json";
    private static final String OFFSET_PATH = "offsets.json";
    private static final String HOST = "192.168.70.203"; // Modbus 서버 IP
    private static final int PORT = Modbus.TCP_PORT; // Modbus TCP 포트
    private static final int SLAVE_ID = 1; // Modbus Slave ID
    private static final int OFFSET = 100; // 읽을 시작 주소
    private static final int QUANTITY = 32; // 읽을 데이터 수

    // 생성자: host, port, JSON 파일 경로들
    public ModbusHandler(String host, int port, int slaveId, int offset, int quantity, String addressFilePath, String channelFilePath, String offsetFilePath) {
        if(host == null || host.isEmpty()){
            throw new IllegalArgumentException("host값을 제대로 입력하세요.");
        }
        if(port <= 0){
            throw new IllegalArgumentException("host값은 음수가 될 수 없습니다.");
        }
        if(slaveId <= 0){
            throw new IllegalArgumentException("slaveId값이 올바르지 않습니다.");
        }
        if(offset < 0){
            throw new IllegalArgumentException("offset값이 올바르지 않습니다.");
        }
        if(quantity < 0){
            throw new IllegalArgumentException("quantity값은 음수가 될 수 없습니다.");
        }
        this.host = host;
        this.port = port;
        this.slaveId = slaveId;
        this.offset = offset;
        this.quantity = quantity;
    }

    public ModbusHandler(){
        this.host = HOST;
        this.port = PORT;
        this.slaveId = SLAVE_ID;
        this.offset = OFFSET;
        this.quantity = QUANTITY;
        this.addressFilePath = ADDRESS_PATH;
        this.channelFilePath = CHANNEL_PATH;
        this.offsetFilePath = OFFSET_PATH;
    }

    // JSON 파일을 로드하는 메서드
    private Map<String, List<Map<String, Object>>> loadJsonFile(String filePath) throws Exception {
        if(filePath.isEmpty()){
            throw new FileNotFoundException("file 경로를 찾을 수 없습니다.");
        }
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(filePath).toURI());
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(file, Map.class);
    }

    // Modbus 데이터를 읽고 Location별로 Map에 저장하는 메서드
    public Map<String, Map<Integer, Double>> readData() {
        Map<String, Map<Integer, Double>> locationData = new HashMap<>();
        ModbusMaster master = null;

        try {
            // JSON 파일에서 데이터 읽기
            Map<String, List<Map<String, Object>>> addressMap = loadJsonFile(addressFilePath);
            Map<String, List<Map<String, Object>>> channelMap = loadJsonFile(channelFilePath);
            Map<String, List<Map<String, Object>>> offsetMap = loadJsonFile(offsetFilePath);

            List<Map<String, Object>> addresses = addressMap.get("addresses");
            List<Map<String, Object>> channels = channelMap.get("channels");
            List<Map<String, Object>> offsets = offsetMap.get("offsets");

            // Modbus 연결 설정
            TcpParameters tcpParameters = new TcpParameters();
            tcpParameters.setHost(InetAddress.getByName(host));
            tcpParameters.setKeepAlive(true);
            tcpParameters.setPort(port);

            master = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
            Modbus.setAutoIncrementTransactionId(true);

            if (!master.isConnected()) {
                master.connect();
            }

            // Location별 데이터를 Map에 저장
            for (Map<String, Object> addressInfo : addresses) {
                int address = (int) addressInfo.get("address");
                String location = (String) addressInfo.get("location");
                int channelId = (int) addressInfo.get("channel");

                // Location별 데이터를 저장할 Map 초기화
                locationData.putIfAbsent(location, new HashMap<>());

                // 해당 채널 정보 찾기
                Map<String, Object> channelInfo = channels.stream()
                        .filter(channel -> (int) channel.get("channel") == channelId)
                        .findFirst().orElse(null);

                if (channelInfo != null) {
                    @SuppressWarnings("unchecked")
                    List<Integer> offsetIds = (List<Integer>) channelInfo.get("offsetIds");

                    // 각 offsetId에 대해 처리
                    for (int offsetId : offsetIds) {
                        Map<String, Object> offsetInfo = offsets.stream()
                                .filter(offset -> (int) offset.get("id") == offsetId)
                                .findFirst().orElse(null);

                        if (offsetInfo != null) {
                            String name = (String) offsetInfo.get("name");
                            int offset = (int) offsetInfo.get("offset");
                            String unit = (String) offsetInfo.get("unit");

                            double scale;
                            Object scaleObj = offsetInfo.get("scale");
                            if (scaleObj instanceof Number) {
                                scale = ((Number) scaleObj).doubleValue();
                            } else {
                                log.error("Invalid scale value for offset {}: {}", offset, scaleObj);
                                continue; // scale이 잘못된 경우 스킵
                            }

                            // Modbus 값 읽기
                            try {
                                int value = master.readInputRegisters(1, address + offset, 1)[0];
                                double formattedValue = value * scale;

                                // Location별 Map에 저장
                                locationData.get(location).put(offset, formattedValue);
                                
                            } catch (ModbusProtocolException | ModbusNumberException | ModbusIOException e) {
                                log.error("Failed to read offset " + offset + " for address " + address, e);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("An error occurred during Modbus data reading", e);
        } finally {
            // Modbus 연결 종료
            if (master != null && master.isConnected()) {
                try {
                    master.disconnect();
                } catch (ModbusIOException e) {
                    log.error("Failed to disconnect Modbus master", e);
                }
            }
        }

        // 결과 Map 반환
        return locationData;
    }

    // 5초마다 데이터를 받아오는 메서드
    public void startDataLoop() {
        while (true) {
            Map<String, Map<Integer, Double>> locationData = readData();
            System.out.println(locationData);

            try {
                // 5초 대기
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                log.error("Error in sleep: ", e);
                break;
            }
        }
    }

    @Override
    public Map<String, Object> transFromMqttMessage(Map<String, Map<Integer, Double>> locationData) {
        if(locationData.isEmpty()){
            throw new IllegalArgumentException("locationData값이 없습니다.");
        }
        Map<String, Object> data = new HashMap<>();

        for (Map.Entry<String, Map<Integer, Double>> entry : locationData.entrySet()) {
            String location = entry.getKey();
            Map<Integer, Double> dataMap = entry.getValue();

            // 각 필드를 초기화
            Map<String, Double> voltageMap = new HashMap<>();
            Map<String, Double> currentMap = new HashMap<>();
            Map<String, Double> powerMap = new HashMap<>();
            Map<String, Double> phaseMap = new HashMap<>();
            Map<String, Double> powerFactorMap = new HashMap<>();

            // 데이터를 원하는 형식으로 변환
            for (Map.Entry<Integer, Double> dataEntry : dataMap.entrySet()) {
                int offset = dataEntry.getKey();
                double value = dataEntry.getValue();

                // offset에 따라 적절한 필드로 값을 할당
                if (offset == 100) {  // 예시로 voltage, current 등으로 매핑
                    voltageMap.put("V1", value);
                } else if (offset == 101) {
                    voltageMap.put("V2", value);
                } else if (offset == 102) {
                    voltageMap.put("V3", value);
                } else if (offset == 200) {
                    currentMap.put("I1", value);
                } else if (offset == 201) {
                    currentMap.put("I2", value);
                } else if (offset == 202) {
                    currentMap.put("I3", value);
                } else if (offset == 300) {
                    powerMap.put("activePower", value);
                } else if (offset == 301) {
                    powerMap.put("reactivePower", value);
                } else if (offset == 302) {
                    powerMap.put("apparentPower", value);
                } else if (offset == 400) {
                    phaseMap.put("phase1", value);
                } else if (offset == 401) {
                    phaseMap.put("phase2", value);
                } else if (offset == 402) {
                    phaseMap.put("phase3", value);
                } else if (offset == 500) {
                    powerFactorMap.put("pf1", value);
                } else if (offset == 501) {
                    powerFactorMap.put("pf2", value);
                } else if (offset == 502) {
                    powerFactorMap.put("pf3", value);
                }
            }

            // 변환된 데이터를 data Map에 추가
            data.put("voltage", voltageMap);
            data.put("current", currentMap);
            data.put("power", powerMap);
            data.put("phase", phaseMap);
            data.put("powerFactor", powerFactorMap);
            data.put("deviceName", location);  // location은 deviceName으로 설정
        }

        return data;
    }
}