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
        // Modbus TCP 연결 설정
        IpParameters params = new IpParameters();
        params.setHost("192.168.70.203");
        params.setPort(502);

        // Modbus Master 생성
        ModbusMaster master = new ModbusFactory().createTcpMaster(params, false);

        try (
                InputStream addressMapStream = App.class.getClassLoader().getResourceAsStream("address_map.json");
                InputStream detailedInfoStream = App.class.getClassLoader().getResourceAsStream("detailed_info.json")) {
            if (addressMapStream == null || detailedInfoStream == null) {
                System.err.println("리소스 파일을 찾을 수 없습니다.");
                return;
            }

            // JSON 파싱
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode addressMap = objectMapper.readTree(addressMapStream);
            JsonNode detailedInfo = objectMapper.readTree(detailedInfoStream);

            // Modbus 통신 초기화
            master.init();

            // address_map.json의 각 주소에 대해 데이터 읽기
            Iterator<String> fieldNames = addressMap.fieldNames();
            while (fieldNames.hasNext()) {
                String addressKey = fieldNames.next();
                JsonNode registerInfo = addressMap.get(addressKey);

                // registerInfo가 null인지 확인
                if (registerInfo == null) {
                    System.err.printf("address_map에서 주소 %s에 대한 정보를 찾을 수 없습니다.%n", addressKey);
                    continue;
                }

                // Size, Scale, Type, Name 확인 및 널 체크
                JsonNode sizeNode = registerInfo.get("Size");
                JsonNode scaleNode = registerInfo.get("Scale");
                JsonNode typeNode = registerInfo.get("Type");
                JsonNode nameNode = registerInfo.get("Name");

                if (sizeNode == null || typeNode == null || nameNode == null) {
                    System.err.printf("주소 %s의 필수 정보 (Size, Type, Name)가 누락되었습니다.%n", addressKey);
                    continue;
                }

                int size = sizeNode.asInt();
                double scale = scaleNode != null && !scaleNode.isNull() ? scaleNode.asDouble() : 1; // 기본값 1
                String type = typeNode.asText();
                String name = nameNode.asText();

                // detailed_info.json에서 상세 이름 가져오기
                String detailedName = detailedInfo.has(addressKey)
                        ? detailedInfo.get(addressKey).asText()
                        : "정보 없음";

                // Modbus 요청 생성 및 전송
                try {
                    ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(1,
                            Integer.parseInt(addressKey), size);
                    ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) master.send(request);

                    if (response.isException()) {
                        System.out.printf("주소 %d (%s): Modbus 오류 - %s%n", Integer.parseInt(addressKey), name,
                                response.getExceptionMessage());
                    } else {
                        short[] values = response.getShortData();
                        double result = convertAndScale(values, type, scale);
                        System.out.printf("주소 %d (%s - %s): %f%n", Integer.parseInt(addressKey), name, detailedName,
                                result);
                    }
                } catch (ModbusTransportException e) {
                    System.err.printf("주소 %d (%s): 통신 오류 - %s%n", Integer.parseInt(addressKey), name, e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            master.destroy(); // 연결 종료
        }
    }

    /**
     * 레지스터 값을 JSON 정의에 따라 변환 및 스케일 적용
     */
    private static double convertAndScale(short[] values, String type, double scale) {
        long rawValue = 0;

        // 레지스터 데이터를 병합 (UINT16, UINT32 처리)
        for (short value : values) {
            rawValue = (rawValue << 16) | (value & 0xFFFF);
        }

        // 타입에 따른 변환
        switch (type) {
            case "UINT16":
            case "UINT32":
                return rawValue / scale; // 스케일 적용
            default:
                System.err.println("알 수 없는 데이터 타입: " + type);
                return rawValue;
        }
    }
}
