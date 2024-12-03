package com.nhnacademy.modbus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.msg.request.ReadInputRegistersRequest;
import com.intelligt.modbus.jlibmodbus.msg.response.ReadInputRegistersResponse;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;

import java.net.InetAddress;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MasterTCP {
    public static Map<Integer, String> addressMapName() {
        Map<Integer, String> map = new HashMap<>();

        map.put(0, "operation Heartbit");
        map.put(1, "temperature");
        map.put(2, "frequency");
        map.put(3, "program version");
        map.put(4, "present CO2 use(month)");
        map.put(6, "operation Heartbit");
        map.put(7, "temperature 1");
        map.put(8, "frequency");
        map.put(9, "program version");
        map.put(10, "present CO2 use(month)");
        map.put(12, "V123(LN) average");
        map.put(13, "V123(LL) average");
        map.put(14, "V123(LN) unbalance");
        map.put(15, "V123(LL) unbalance");
        map.put(16, "V1");
        map.put(17, "V12");
        map.put(18, "V1 unbalance");
        map.put(19, "V12 unbalance");
        map.put(20, "V2");
        map.put(21, "V23");
        map.put(22, "V2 unbalance");
        map.put(23, "V23 unbalance");
        map.put(24, "V3");
        map.put(25, "V31");
        map.put(26, "V3 unbalance");
        map.put(27, "V31 unbalance");
        map.put(28, "V1 THD");
        map.put(29, "V2 THD");
        map.put(30, "V3 THD");

        return map;
    }

    public static int transformToInt(int address, int value) {
        if (address == 0 || address == 3 || address == 6 || address == 9) {
            return value;
        }
        return -1;
    }

    private static int applyScale(int address, int value) {
        if (address == 0 || address == 3 || address == 6 || address == 9) {
            return value;
        } else if (address == 1 || address == 4 || address == 7 || address == 10) {
            return value / 10;
        } else {
            return value / 100;
        }
    }

    // offset에 해당하는 이름을 얻는 메서드
    private static String getTopicForOffset(int offset) {
        switch (offset) {
            case 100:
                return TOPIC + "/캠퍼스";
            case 200:
                return TOPIC + "/캠퍼스1";
            case 300:
                return TOPIC + "/전등1";
            case 400:
                return TOPIC + "/전등2";
            case 500:
                return TOPIC + "/전등3";
            case 600:
                return TOPIC + "/강의실b_바닥";
            case 700:
                return TOPIC + "/강의실a_바닥1";
            case 800:
                return TOPIC + "/강의실a_바닥2";
            case 900:
                return TOPIC + "/프로젝터1";
            case 1000:
                return TOPIC + "/프로젝터2";
            case 1100:
                return TOPIC + "/회의실";
            case 1200:
                return TOPIC + "/서버실";
            case 1300:
                return TOPIC + "/간판";
            case 1400:
                return TOPIC + "/페어룸";
            case 1500:
                return TOPIC + "/사무실_전열1";
            case 1600:
                return TOPIC + "/사무실_전열2";
            case 1700:
                return TOPIC + "/사무실_복사기";
            case 1800:
                return TOPIC + "/빌트인_전열";
            case 1900:
                return TOPIC + "/사무실_전열3";
            case 2000:
                return TOPIC + "/정수기";
            case 2100:
                return TOPIC + "/하이브_전열";
            case 2200:
                return TOPIC + "/바텐_전열";
            case 2300:
                return TOPIC + "/S_P";
            case 2400:
                return TOPIC + "/공조기";
            case 2500:
                return TOPIC + "/AC";
            default:
                return TOPIC;
        }
    }

    private static final String BROKER = "tcp://192.168.71.202:1883"; // MQTT 브로커 주소
    private static final String CLIENT_ID = "atgn002"; // 클라이언트 ID
    private static final String TOPIC = "application/modbus"; // 발행 주제

    public static void main(String[] args) {

        Modbus.log().addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                System.out.println(record.getLevel().getName() + ": " + record.getMessage());
            }

            @Override
            public void flush() {
                // do nothing
            }

            @Override
            public void close() throws SecurityException {
                // do nothing
            }
        });
        Modbus.setLogLevel(Modbus.LogLevel.LEVEL_DEBUG);

        try (MqttClient client = new MqttClient(BROKER, CLIENT_ID)) {
            // MQTT 연결 설정
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            client.connect(options);

            TcpParameters tcpParameters = new TcpParameters();
            tcpParameters.setHost(InetAddress.getByName("192.168.70.203"));
            tcpParameters.setKeepAlive(true);
            tcpParameters.setPort(Modbus.TCP_PORT);

            ModbusMaster m = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
            Modbus.setAutoIncrementTransactionId(true);

            int slaveId = 1;
            int offset = 100; // 99번째의 주소 부터 조회한다
            int quantity = 32; // 0번째 부터 순서대로 31번째 주소를 조회한다.

            try {
                // since 1.2.8
                if (!m.isConnected()) {
                    m.connect();
                }

                for (offset = 100; offset <= 2400; offset += 100) {
                    ReadInputRegistersRequest request = new ReadInputRegistersRequest();
                    request.setServerAddress(slaveId);
                    request.setStartAddress(offset);
                    request.setQuantity(quantity);
                    request.setTransactionId(1);
                    ReadInputRegistersResponse response = (ReadInputRegistersResponse) m.processRequest(request);

                    Map<Integer, String> addressNames = addressMapName();

                    // JSON 객체 준비
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> payloadData = new HashMap<>();
                    payloadData.put("deviceId", slaveId);
                    payloadData.put("timestamp", Instant.now().toString());
                    Map<String, Object> measurements = new HashMap<>();

                    // you can get either int[] containing register values or byte[] containing raw
                    // bytes.
                    for (int i = 0; i < response.getHoldingRegisters().getQuantity(); i++) {
                        // addressNames에서 해당 주소가 있는지 확인
                        if (!addressNames.containsKey(i)) {
                            continue;
                        }

                        String name = addressNames.get(i);
                        int value;

                        // 32비트 주소 (16, 17, 20, 21, 24, 25번 주소 등)
                        if (i == 12 || i == 13 || i == 16 || i == 17 || i == 20 || i == 21 || i == 24 || i == 25) {
                            value = (response.getHoldingRegisters().get(i) << 16)
                                    | response.getHoldingRegisters().get(i + 1);
                            measurements.put(name, applyScale(i, value));
                        } else {
                            value = response.getHoldingRegisters().get(i);
                            measurements.put(name, applyScale(i, value));
                        }
                    }

                    payloadData.put("measurements", measurements);

                    // MQTT 메시지 발행
                    String payload = objectMapper.writeValueAsString(payloadData);
                    MqttMessage message = new MqttMessage(payload.getBytes());
                    String topic = getTopicForOffset(offset);
                    client.publish(topic, message);
                    System.out.println("Published message to MQTT: " + payload);

                    Thread.sleep(1000);
                }

            } catch (ModbusProtocolException | ModbusNumberException | ModbusIOException e) {
                e.printStackTrace();
            } finally {
                try {
                    m.disconnect();
                } catch (ModbusIOException e) {
                    e.printStackTrace();
                }
            }

            client.disconnect();
            System.out.println("Disconnected from MQTT Broker!");

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
