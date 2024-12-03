package com.sensor_data_parsing;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.msg.request.ReadInputRegistersRequest;
import com.intelligt.modbus.jlibmodbus.msg.response.ReadInputRegistersResponse;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;

public class ModbusPublish {
    // 메시지를 보낼 브로커
    private static final String newMqttHost = "localhost"; // MQTT 브로커 주소
    private static final String newMqttUsername = ""; // MQTT 사용자 이름
    private static final String newMqttPassword = ""; // MQTT 비밀번호

    private static final Map<Integer, String[]> addressMap = new HashMap<>();

    static {
        addressMap.put(0, new String[] { "type", "1" });
        addressMap.put(1, new String[] { "a leakage current", "1" });
        addressMap.put(2, new String[] { "current", "100.0" });
        addressMap.put(4, new String[] { "W", "1" });
        addressMap.put(6, new String[] { "VAR", "1" });
        addressMap.put(8, new String[] { "VA", "1" });
        addressMap.put(10, new String[] { "PF average", "100.0" });
        addressMap.put(11, new String[] { "reserved", "1" });
        addressMap.put(12, new String[] { "current unbalance", "100.0" });
        addressMap.put(13, new String[] { "I THD average", "100.0" });
        addressMap.put(14, new String[] { "IGR", "10.0" });
        addressMap.put(15, new String[] { "IGC", "10.0" });
        addressMap.put(16, new String[] { "V1", "100.0" });
        addressMap.put(18, new String[] { "I1", "100.0" });
        addressMap.put(20, new String[] { "W", "1" });
        addressMap.put(22, new String[] { "VAR", "1" });
        addressMap.put(24, new String[] { "VA", "1" });
        addressMap.put(26, new String[] { "volt unbalance", "100.0" });
        addressMap.put(27, new String[] { "current unbalance", "100.0" });
        addressMap.put(28, new String[] { "phase", "100.0" });
        addressMap.put(29, new String[] { "power factor", "100.0" });
        addressMap.put(30, new String[] { "I1 THD", "100.0" });
        addressMap.put(31, new String[] { "reserved", "1" });
        addressMap.put(32, new String[] { "V2", "100.0" });
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

    public static void main(String[] args) {
        final Mqtt5Client newMqttClient = Mqtt5Client.builder()
                .identifier("controlcenter-5678") // 클라이언트 식별자
                .serverHost(newMqttHost)
                .automaticReconnectWithDefaultConfig() // 자동 재연결
                .serverPort(8888)
                .build();

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

        try {
            // 메시지를 보낼 새로운 MQTT 클라이언트 연결
            newMqttClient.toBlocking().connectWith()
                    .simpleAuth()
                    .username(newMqttUsername)
                    .password(newMqttPassword.getBytes(StandardCharsets.UTF_8))
                    .applySimpleAuth()
                    .cleanStart(false)
                    .sessionExpiryInterval(TimeUnit.HOURS.toSeconds(1))
                    .send();

            TcpParameters tcpParameters = new TcpParameters();
            // tcp parameters have already set by default as in example
            tcpParameters.setHost(InetAddress.getByName("192.168.70.203"));
            tcpParameters.setKeepAlive(true);
            tcpParameters.setPort(Modbus.TCP_PORT);

            // if you would like to set connection parameters separately,
            // you should use another method: createModbusMasterTCP(String host, int port,
            // boolean keepAlive);
            ModbusMaster m = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
            Modbus.setAutoIncrementTransactionId(true);

            int slaveId = 1;
            int offset = 100;
            int quantity = 32;

            try {
                if (!m.isConnected()) {
                    m.connect();
                }

                while (offset <= 2400) {
                    System.out.println(channelMap.get(offset));

                    ReadInputRegistersRequest request = new ReadInputRegistersRequest();
                    request.setServerAddress(slaveId);
                    request.setStartAddress(offset);
                    request.setQuantity(quantity);
                    request.setTransactionId(1);

                    ReadInputRegistersResponse response = (ReadInputRegistersResponse) m.processRequest(request);

                    // 메시지를 보낼 새 MQTT 브로커로 메시지 전송
                    String newTopic = "application/modbus";

                    Map<String, Object> dataMap = new HashMap<>();

                    // 레지스터 값을 읽고 처리
                    for (int i = 0; i < response.getHoldingRegisters().getQuantity(); i++) {
                        int registerValue = response.getHoldingRegisters().get(i);
                        int address = offset + i;

                        if ((address - offset == 2 ||
                                address - offset == 4 ||
                                address - offset == 6 ||
                                address - offset == 8 ||
                                address - offset == 16 ||
                                address - offset == 18 ||
                                address - offset == 20 ||
                                address - offset == 22 ||
                                address - offset == 24 ||
                                address - offset == 32) &&
                                i < response.getHoldingRegisters().getQuantity() - 1) {
                            registerValue = ((response.getHoldingRegisters().get(i) << 16)
                                    | response.getHoldingRegisters().get(i + 1));
                            i++; // 두 개의 레지스터를 처리했으므로 i를 1 증가시킴
                        }

                        String addressDescription = addressMap.get(address - offset)[0];
                        double registerValueDouble = registerValue
                                / Double.parseDouble(addressMap.get(address - offset)[1]);
                        if (registerValueDouble == Math.floor(registerValueDouble)) {
                            // registerValueDouble이 정수와 같으면 int로 변환
                            int registerValueInt = (int) registerValueDouble;
                            System.out.println("Address: " + address + ", Value: " + registerValueInt); // int 출력
                            dataMap.put(addressDescription, registerValueInt); // int 값 저장
                        } else {
                            // 그렇지 않으면 double로 처리
                            System.out.println("Address: " + address + ", Value: " + registerValueDouble); // double 출력
                            dataMap.put(addressDescription, registerValueDouble); // double 값 저장
                        }
                    }

                    Map<String, Object> dataMessage = new HashMap<>();
                    dataMessage.put("deviceName", channelMap.get(offset));
                    dataMessage.put("data", dataMap);
                    ObjectMapper objectMapper = new ObjectMapper();
                    String message = objectMapper.writeValueAsString(dataMessage);

                    // 메시지를 보낼 새 MQTT 브로커로 메시지 발행
                    newMqttClient.toAsync().publishWith()
                            .topic(newTopic)
                            .payload(message.getBytes(StandardCharsets.UTF_8))
                            .qos(MqttQos.AT_LEAST_ONCE) // QoS 1 (최소 한 번 전송)
                            .send();

                    offset += 100;
                    System.out.println();
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
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
