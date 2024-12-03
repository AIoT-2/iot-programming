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

    private static final Map<Integer, String> addressMap = new HashMap<>();
    static {
        addressMap.put(0, "operation Heartbit");
        addressMap.put(1, "temperature");
        addressMap.put(2, "frequency");
        addressMap.put(3, "program version");
        addressMap.put(4, "present CO2 use(month)");
        addressMap.put(6, "operation Heartbit");
        addressMap.put(7, "temperature 1");
        addressMap.put(8, "frequency");
        addressMap.put(9, "program version");
        addressMap.put(10, "present CO2 use(month)");
        addressMap.put(12, "V123(LN) average");
        addressMap.put(13, "V123(LL) average");
        addressMap.put(14, "V123(LN) unbalance");
        addressMap.put(15, "V123(LL) unbalance");
        addressMap.put(16, "V1");
        addressMap.put(17, "V12");
        addressMap.put(18, "V1 unbalance");
        addressMap.put(19, "V12 unbalance");
        addressMap.put(20, "V2");
        addressMap.put(21, "V23");
        addressMap.put(22, "V2 unbalance");
        addressMap.put(23, "V23 unbalance");
        addressMap.put(24, "V3");
        addressMap.put(25, "V31");
        addressMap.put(26, "V3 unbalance");
        addressMap.put(27, "V31 unbalance");
        addressMap.put(28, "V1 THD");
        addressMap.put(29, "V2 THD");
        addressMap.put(30, "V3 THD");
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
                // since 1.2.8
                if (!m.isConnected()) {
                    m.connect();
                }

                ReadInputRegistersRequest request = new ReadInputRegistersRequest();
                request.setServerAddress(slaveId);
                request.setStartAddress(offset);
                request.setQuantity(quantity);
                request.setTransactionId(1);

                ReadInputRegistersResponse response = (ReadInputRegistersResponse) m.processRequest(request);

                // 메시지를 보낼 새 MQTT 브로커로 메시지 전송
                String newTopic = "application/modbus";

                Map<String, Object> dataMap = new HashMap<>();

                int addressIncrement = 0;
                // 레지스터 값을 읽고 처리
                for (int i = 0; i < response.getHoldingRegisters().getQuantity(); i++) {
                    int registerValue = response.getHoldingRegisters().get(i);
                    if (i == 5 || i == 10) {
                        addressIncrement++;
                    }
                    int address = offset + addressIncrement; // 주소는 i에 맞게 증가

                    if ((address - offset == 12 ||
                            address - offset == 13 ||
                            address - offset == 16 ||
                            address - offset == 17 ||
                            address - offset == 20 ||
                            address - offset == 21 ||
                            address - offset == 24 ||
                            address - offset == 25) &&
                            i < response.getHoldingRegisters().getQuantity() - 1) {
                        registerValue = (response.getHoldingRegisters().get(i) << 16)
                                | response.getHoldingRegisters().get(i + 1);
                        i++; // 두 개의 레지스터를 처리했으므로 i를 1 증가시킴
                    }

                    System.out.println("Address: " + address + ", Value: " + registerValue);

                    String addressDescription = addressMap.get(address - offset);
                    dataMap.put(addressDescription, registerValue);
                    addressIncrement++;
                }

                Map<String, Object> dataMessage = new HashMap<>();
                dataMessage.put("deviceName", "캠퍼스");
                dataMessage.put("data", dataMap);
                ObjectMapper objectMapper = new ObjectMapper();
                String message = objectMapper.writeValueAsString(dataMessage);

                // 메시지를 보낼 새 MQTT 브로커로 메시지 발행
                newMqttClient.toAsync().publishWith()
                        .topic(newTopic)
                        .payload(message.getBytes(StandardCharsets.UTF_8))
                        .qos(MqttQos.AT_LEAST_ONCE) // QoS 1 (최소 한 번 전송)
                        .send();
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
