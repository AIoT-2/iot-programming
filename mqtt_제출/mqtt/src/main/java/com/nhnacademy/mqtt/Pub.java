package com.nhnacademy.mqtt;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.config.ModbusConfig;
import com.nhnacademy.config.MqttConfig;
import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersRequest;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Pub implements Runnable {

    private MqttClient mqttClient;
    private ModbusMaster master;
    private Map<Integer, String> addressMap = new HashMap<>();
    private Map<Integer, Map<String, Object>> offsetMap = new HashMap<>();
    private final long publishInterval = 10000; // 발행 주기 (10초)

    public Pub(MqttConfig mqttConfig, ModbusConfig modbusConfig)
            throws MqttException, ModbusInitException {
        // 고유한 clientId 생성
        String uniqueClientId = mqttConfig.getClientId() + "-" + UUID.randomUUID().toString();

        // MQTT 클라이언트 초기화
        this.mqttClient = new MqttClient(mqttConfig.getBroker(), uniqueClientId);
        this.mqttClient.connect();

        // Modbus TCP 연결 설정
        IpParameters params = new IpParameters();
        params.setHost(modbusConfig.getHost());
        params.setPort(modbusConfig.getPort());
        this.master = new ModbusFactory().createTcpMaster(params, false);
        this.master.init();

        // JSON 파일을 읽어 addressMap과 offsetMap을 초기화
        loadConfig();
    }

    private void loadConfig() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // 클래스패스에서 파일을 로드
            InputStream jsonFileStream =
                    getClass().getClassLoader().getResourceAsStream("channels.json");
            InputStream jsonFileStream2 =
                    getClass().getClassLoader().getResourceAsStream("channelInfo.json");

            if (jsonFileStream == null || jsonFileStream2 == null) {
                log.error("Required JSON files not found in the classpath.");
                return;
            }

            List<Map<String, Object>> dataList = objectMapper.readValue(jsonFileStream, List.class);
            List<Map<String, Object>> dataList2 =
                    objectMapper.readValue(jsonFileStream2, List.class);

            // Address 데이터 매핑
            for (Map<String, Object> item : dataList) {
                int address = (Integer) item.get("address");
                String place = (String) item.get("place");
                addressMap.put(address, place);
            }

            // Offset 데이터 매핑
            for (Map<String, Object> item : dataList2) {
                int offset = (Integer) item.get("Offset");
                item.remove("Offset");
                offsetMap.put(offset, item);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void publishModbusData() {
        for (Integer address : addressMap.keySet()) {
            for (Integer offset : offsetMap.keySet()) {
                processModbusData(address, offset);
            }
        }
    }

    private void processModbusData(Integer address, Integer offset) {
        try {
            int slaveId = 1;
            int channel = address;
            int offsetAddr = offset;
            int numberOfRegisters = (int) offsetMap.get(offset).get("Size");
            int scale = (int) offsetMap.get(offset).get("Scale");
            String type = offsetMap.get(offset).get("Type").toString();

            // Holding Registers 요청 생성 및 전송
            ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(slaveId,
                    channel + offsetAddr, numberOfRegisters);
            ReadHoldingRegistersResponse response =
                    (ReadHoldingRegistersResponse) master.send(request);

            if (response == null || response.isException()) {
                log.debug("Modbus 오류: {}",
                        response != null ? response.getExceptionMessage() : "응답 없음");
                return;
            }

            double value = parseModbusResponse(response, type, scale);
            if (value != 0) {
                String formattedValue = formatValue(value);
                String topic = createMqttTopic(address, offset);
                String payload = createMqttPayload(formattedValue);

                // MQTT 메시지 발행
                publishToMqtt(topic, payload);
            }
        } catch (ModbusTransportException e) {
            log.error("Modbus 전송 오류", e);
        }
    }

    private double parseModbusResponse(ReadHoldingRegistersResponse response, String type,
            int scale) {
        short[] values = response.getShortData();
        double value = 0;

        if (values.length == 1) {
            value = (values[0] < 0 && type.equals("uint16")) ? (values[0] & 0xFFFF) / (double) scale
                    : values[0] / (double) scale;
        } else if (values.length == 2) {
            long highPart = (values[0] & 0xFFFF) << 16;
            long lowPart = (values[1] & 0xFFFF);
            value = (highPart | lowPart) / (double) scale;
        }

        return value;
    }

    private String formatValue(double value) {
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        return decimalFormat.format(value);
    }

    private String createMqttTopic(Integer address, Integer offset) {
        return "dongdong/s/nhnacademy/b/gyeongnam_campus/p/" + addressMap.get(address) + "/e/"
                + offsetMap.get(offset).get("Name");
    }

    private String createMqttPayload(String formattedValue) {
        return String.format("{\"time\":%d,\"value\":%s}", System.currentTimeMillis(),
                formattedValue);
    }

    private void publishToMqtt(String topic, String payload) {
        try {
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(1);
            mqttClient.publish(topic, message);
            log.info("Published to {}: {}", topic, payload);
        } catch (MqttException e) {
            log.error("MQTT 발행 오류", e);
        }
    }

    public void close() {
        try {
            master.destroy();
            mqttClient.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                publishModbusData();
                Thread.sleep(publishInterval); // 지정된 시간 간격으로 데이터 발행
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}

