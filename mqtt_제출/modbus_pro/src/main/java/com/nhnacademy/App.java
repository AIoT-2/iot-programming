package com.nhnacademy;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersRequest;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersResponse;

/**
 * Hello world!
 *
 */
public class App {


    public static void main(String[] args) {
        // Modbus TCP 연결 설정
        IpParameters params = new IpParameters();
        params.setHost("192.168.70.203");
        params.setPort(502);

        // Modbus Master 생성
        ModbusMaster master = new ModbusFactory().createTcpMaster(params, false);

        ObjectMapper objectMapper = new ObjectMapper();

        Map<Integer, String> addressMap = new java.util.HashMap<>();
        Map<Integer, Map<String, Object>> offsetMap = new HashMap<>();
        try {
            File jsonFile = new File("src/resources/channels.json");
            File jsonFile2 = new File("src/resources/channelInfo.json");
            // JSON을 List<Map> 형태로 변환
            List<Map<String, Object>> dataList = objectMapper.readValue(jsonFile, List.class);
            List<Map<String, Object>> dataList2 = objectMapper.readValue(jsonFile2, List.class);

            // Address 데이터 매핑
            for (Map<String, Object> item : dataList) {
                int address = (Integer) item.get("address");
                String place = (String) item.get("place");
                addressMap.put(address, place);
            }

            // Offset 데이터 매핑
            for (Map<String, Object> item : dataList2) {
                int offset = (Integer) item.get("Offset");
                item.remove("Offset"); // Offset을 제거하고
                offsetMap.put(offset, item); // 나머지 데이터를 Map에 저장
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String broker = "tcp://192.168.70.203:1883";
        String clientId = "dongdongModbus";
        try (MqttClient mqttClient = new MqttClient(broker, clientId)) {
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            mqttClient.connect(connOpts);

            System.out.println("Connected to MQTT borker");

            while (true) {
                try {
                    for (Integer key : addressMap.keySet()) {
                        for (Integer key2 : offsetMap.keySet()) {
                            try {
                                master.init();

                                int slaveId = 1;
                                int channel = key;
                                int offset = key2;
                                int numberOfRegisters = (int) offsetMap.get(key2).get("Size");
                                int scale = (int) offsetMap.get(key2).get("Scale");
                                String type = offsetMap.get(key2).get("Type").toString();

                                // Holding Registers 요청 생성
                                ReadHoldingRegistersRequest request =
                                        new ReadHoldingRegistersRequest(slaveId, channel + offset,
                                                numberOfRegisters);
                                // 요청 전송 및 응답 수신
                                ReadHoldingRegistersResponse response =
                                        (ReadHoldingRegistersResponse) master.send(request);

                                // 응답 처리
                                if (response.isException()) {
                                    System.out.println(
                                            "Modbus 오류: " + response.getExceptionMessage());
                                } else {
                                    short[] values = response.getShortData();
                                    double value = 0;
                                    if (values.length == 1) {
                                        if (values[0] < 0 && type.equals("uint16")) {
                                            value = (double) (values[0] & 0xFFFF) / scale;
                                        } else {
                                            value = (double) values[0] / scale;
                                        }
                                    } else {
                                        long highPart = (values[0] & 0xFFFF) << 16;
                                        long lowPart = (values[1] & 0xFFFF);
                                        value = (double) (highPart | lowPart) / scale;
                                    }

                                    DecimalFormat decimalFormat = new DecimalFormat("#.##");
                                    String formattedValue = decimalFormat.format(value);

                                    if (value != 0) {
                                        String topic = "dongdong/" + "s/" + "nhnacademy/" + "b/"
                                                + "gyeongnam_campus/" + "p/" + addressMap.get(key)
                                                + "/" + "e/" + offsetMap.get(key2).get("Name");
                                        String payload = String.format("{\"time\":%d,\"value\":%s}",
                                                System.currentTimeMillis(), formattedValue);

                                        // MQTT 메시지 생성 및 발행
                                        MqttMessage message = new MqttMessage(payload.getBytes());
                                        message.setQos(1);

                                        mqttClient.publish(topic, message);
                                        System.out.printf("Published to %s: %s\n", topic, payload);
                                    }
                                }
                            } catch (ModbusInitException ie) {
                                ie.printStackTrace();
                            } catch (ModbusTransportException e) {
                                e.printStackTrace();
                            } finally {
                                master.destroy(); // 연결 종료
                            }
                        }
                    }

                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
