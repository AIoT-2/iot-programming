package com.nhnacademy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersRequest;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersResponse;

import java.io.InputStream;
import java.util.Iterator;

public class App {
    public static void main(String[] args) {
        IpParameters params = new IpParameters();
        params.setHost("192.168.70.203");
        params.setPort(502);

        ModbusMaster master = new ModbusFactory().createTcpMaster(params, false);

        try (
                InputStream addressMapStream = App.class.getClassLoader().getResourceAsStream("address_map.json");
                InputStream detailedInfoStream = App.class.getClassLoader().getResourceAsStream("detailed_info.json")) {
            if (addressMapStream == null || detailedInfoStream == null) {
                System.err.println("리소스 파일을 찾을 수 없습니다.");
                return;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode addressMap = objectMapper.readTree(addressMapStream);
            JsonNode detailedInfo = objectMapper.readTree(detailedInfoStream);

            master.init();

            Iterator<String> addressKeys = addressMap.fieldNames();
            while (addressKeys.hasNext()) {
                String addressKey = addressKeys.next();
                int baseAddress = Integer.parseInt(addressKey);
                String locationName = addressMap.get(addressKey).asText();

                System.out.printf("=== 주소 %d (%s) 데이터 ===%n", baseAddress, locationName);

                Iterator<String> detailedKeys = detailedInfo.fieldNames();
                while (detailedKeys.hasNext()) {
                    String detailedKey = detailedKeys.next();
                    JsonNode registerInfo = detailedInfo.get(detailedKey);

                    int offset = Integer.parseInt(detailedKey);
                    int registerAddress = baseAddress + offset;
                    String name = registerInfo.get("Name").asText();
                    String type = registerInfo.get("Type").asText();
                    int size = registerInfo.get("Size").asInt();
                    double scale = registerInfo.has("Scale") && !registerInfo.get("Scale").isNull()
                            ? registerInfo.get("Scale").asDouble()
                            : 1;

                    try {
                        ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(1, registerAddress, size);
                        ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) master.send(request);

                        if (response.isException()) {
                            System.out.printf("주소 %d (%s): Modbus 오류 - %s%n", registerAddress, name,
                                    response.getExceptionMessage());
                        } else {
                            short[] values = response.getShortData();
                            double result = convertAndScale(values, type, scale);

                            if (result != 0) {
                                if (result < 0) {
                                    result = Math.abs(result);
                                }
                                System.out.printf("주소 %d (%s - %s): %f%n", registerAddress, locationName, name, result);
                            }
                        }
                    } catch (ModbusTransportException e) {
                        System.err.printf("주소 %d (%s): 통신 오류 - %s%n", registerAddress, name, e.getMessage());
                    }
                }
                System.out.println();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            master.destroy();
        }
    }

    private static double convertAndScale(short[] values, String type, double scale) {
        long rawValue = 0;

        for (short value : values) {
            rawValue = (rawValue << 16) | (value & 0xFFFF);
        }

        switch (type) {
            case "UINT16":
            case "UINT32":
                return rawValue / scale;
            default:
                System.err.println("알 수 없는 데이터 타입: " + type);
                return rawValue;
        }
    }
}
