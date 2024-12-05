package com.nhnacademy.mtqq.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Re {
    private static final Logger log = LoggerFactory.getLogger(Re.class);

    private String host;
    private int port;
    private String addressFilePath;
    private String channelFilePath;
    private String offsetFilePath;

    private String HOST = "192.168.70.203"; // Modbus 서버 IP
    private int PORT = Modbus.TCP_PORT; // Modbus TCP 포트
    private int SLAVE_ID = 1; // Modbus Slave ID
    private int OFFSET = 100; // 읽을 시작 주소
    private int QUANTITY = 32;

    // 생성자: host, port, JSON 파일 경로들
    public Re(String host, int port, String addressFilePath, String channelFilePath, String offsetFilePath) {
        this.host = host;
        this.port = port;
        this.addressFilePath = addressFilePath;
        this.channelFilePath = channelFilePath;
        this.offsetFilePath = offsetFilePath;
    }

    // JSON 파일을 로드하는 메서드
    private Map<String, List<Map<String, Object>>> loadJsonFile(String filePath) throws Exception {
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

                                //System.out.printf("Location: %s, Offset: %d, Value: %.2f %s%n", location, offset, formattedValue, unit);
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
            // 여기서 locationData를 활용할 수 있습니다.
            // 예: locationData 출력
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
}