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
import com.nhnacademy.settings.DemoSetting;

import java.net.InetAddress;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MasterTCP {
    public static void main(String[] args) {
        Modbus.log().addHandler(new Handler() {
            @Override
            public void publish(LogRecord records) {
                log.debug("{}: {}", records.getLevel().getName(), records.getMessage());
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

        try (MqttClient client = new MqttClient(DemoSetting.BROKER, DemoSetting.CLIENT_ID)) {
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

                    Map<Integer, String[]> addressNames = ConfigurationData.addressMapName();

                    // JSON 객체 준비
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> payloadData = new HashMap<>();
                    payloadData.put("deviceId", slaveId);
                    payloadData.put("timestamp", Instant.now().toString());
                    Map<String, Object> measurements = new HashMap<>();

                    for (int i = 0; i < response.getHoldingRegisters().getQuantity(); i++) {
                        // addressNames에서 해당 주소가 있는지 확인
                        if (!addressNames.containsKey(i)) {
                            continue;
                        }
                        Map<Integer, String[]> registerMap = ConfigurationData.addressMapName();
                        String[] registerInfo = registerMap.get(i);

                        String name = registerInfo[0];

                        int combineRegister = (response.getHoldingRegisters().get(i) << 16)
                                | response.getHoldingRegisters().get(i + 1);
                        int value;

                        // 32비트 주소
                        if (registerInfo != null && "32".equals(registerInfo[2])) {
                            value = combineRegister;
                        } else {
                            value = response.getHoldingRegisters().get(i);
                        }

                        // value가 0이면 값을 넣지 않는다.
                        if (value != 0) {
                            measurements.put(name, ConfigurationData.applyScale(i, value));
                        }
                    }

                    payloadData.put("measurements", measurements);

                    // MQTT 메시지 발행
                    String payload = objectMapper.writeValueAsString(payloadData);
                    MqttMessage message = new MqttMessage(payload.getBytes());

                    Map<Integer, String> topciMap = ConfigurationData.topicMapName();
                    if (topciMap.containsKey(offset)) {
                        client.publish(topciMap.get(offset), message);
                    } else {
                        client.publish(DemoSetting.MODBUS_TOPIC, message);
                    }

                    log.debug("Published message to MQTT: {}", payload);

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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
