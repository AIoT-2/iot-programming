package com.totalmqtt;

import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.msg.request.ReadHoldingRegistersRequest;
import com.intelligt.modbus.jlibmodbus.msg.response.ReadHoldingRegistersResponse;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;

import lombok.extern.slf4j.Slf4j;

import java.io.FileReader;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

// TOPIC, 주소를 바꿀 수 있게 수정해야함
// try를 여러번 쌓는 것은 최대한 자제
// system.out 대신 log로 변환
// Testing해보기

@Slf4j
public class Modbus2 implements Runnable{
    // private int[] transBit =
    // {2,4,6,8,16,18,20,22,24,32,34,36,38,40,48,70,52,54,56};

    private int[] transBit = { 2, 4, 6, 8, 16, 18, 20, 22, 24 }; // 16bit를 32bit로 변환해야할 번호
    private Map<Integer, Integer> scale; // 각 quantity에 따른 데이터의 스케일
    private String ip;
    private int port;
    private TcpParameters tcpParameters;
    private ModbusMaster m;
    private int slaveId;
    private int offset; // 위치 값에 따른 숫자
    private int quantity; // 위치에 해당하는 가져올 채널의 개수
    private ReadHoldingRegistersRequest request;
    private ReadHoldingRegistersResponse response;
    private int start, end, step;
    
    public Modbus2() {
        start = end = step = 1;
        ip = "192.168.70.203";
        port = Modbus.TCP_PORT;
        scale = new HashMap<Integer, Integer>() {
            {
                put(2, 100);
                put(10, 100);
                put(12, 100);
                put(13, 100);
                put(14, 10);
                put(15, 10);
                put(16, 100);
                put(18, 100);
                put(26, 100);
                put(27, 100);
                put(28, 100);
                put(29, 100);
                put(30, 100);
            }
        };

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
            tcpParameters = new TcpParameters();
            tcpParameters.setHost(InetAddress.getByName(ip)); // 주소설정
            tcpParameters.setKeepAlive(true); // 연결유지설정
            tcpParameters.setPort(port); // 포트설정

            m = ModbusMasterFactory.createModbusMasterTCP(tcpParameters); // TCP를 사용하는 Modbus를 생성
            Modbus.setAutoIncrementTransactionId(true); // 각 요청에 대해 Transaction 자동 증가

            // Modbus 슬레이브 장치의 ID / 시작 주소 / 읽을 레지스터 수 지정
            slaveId = 1;
            offset = 100;
            quantity = 32;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void settingInformation(String ip, int port){
        this.ip = ip;
        this.port = port;
    }

    public void settingIterator(int start, int end, int step){
        this.start = start;
        this.end = end;
        this.step = step;
    }

    public void load(int offset) {
        try {
            this.offset = offset; // 위치 설정
            String locationName = Location(offset);
            // since 1.2.8
            if (!m.isConnected()) {
                m.connect();
            }

            int[] registerValues = m.readHoldingRegisters(slaveId, offset, quantity); // 32

            request = new ReadHoldingRegistersRequest(); // 마스터가 슬래이브에게 Read Holding Registers를 요청함
            request.setServerAddress(slaveId); // ID 1번의 창치로 전달
            request.setStartAddress(offset); // 지정한 주소부터 시작
            request.setQuantity(quantity);
            request.setTransactionId(1);
            response = (ReadHoldingRegistersResponse) m.processRequest(request);

            getData(); // 데이터를 비트변환 및 구분

        } catch (ModbusProtocolException e) {
            e.printStackTrace();
        } catch (ModbusNumberException e) {
            e.printStackTrace();
        } catch (ModbusIOException e) {
            e.printStackTrace();
        } finally {
            try {
                m.disconnect();
            } catch (ModbusIOException e) {
                e.printStackTrace();
            }
        }
    }

    private String Location(int channel) { // 위치 채널에 따른 위치 이름
        channel /= 100;
        channel--;
        String[] loca = { "캠퍼스", "캠퍼스1", "전등1", "전등2", "전등3", "강의실b_바닥", "강의실a_바닥1", "강의실a_바닥2", "프로젝터1",
                "프로젝터2", "회의실", "서버실", "간판", "페어룸", "사무실_전열1", "사무실_전열2", "사무실_복사기", "빌트인_전열", "정수기", "하이브_전열", "바텐_전열",
                "S_P", "공조기", "AC" };
        return loca[channel];
    }

    private String JsonName(int channel){
        String[] jsonName = { "type", "a_leakage_current", "current", "W", "VAR", "VA", "PF_average", "reserved",
            "current_unbalance", "l_THD_average", "IGR", "IGC", "V1", "l1", "W1", "VAR1", "VA1", "volt_unbalance",
            "current_unbalace1", "phase", "power_factor", "l1_THD", "reserved1" };
        return jsonName[channel];
    }

    private void getData() { // 각 데이터를 가져옴
        try {
            // 저장할 변수 선언
            Map<String, Object> toJson = new HashMap<>();
            int cnt = 0;
            double value = 0;
            for (int i = 0; i < quantity; i++) {
                value = response.getHoldingRegisters().get(i);
                boolean check = false;
                for (int h = 0; h < transBit.length; h++) { // 32bit 변환 검사
                    if (transBit[h] == i) {// i위치가 transBit에 해당하는지 확인
                        int intValue = response.getHoldingRegisters().get(i) << 16
                                | response.getHoldingRegisters().get(++i);
                        value = intValue;
                        check = true;
                        break;
                    }
                }

                Integer scaleValue = null;
                if (!check) scaleValue = scale.get(i);
                else scaleValue = scale.get(i-1);

                if (scaleValue != null) value = value / scaleValue.intValue(); // 스케일을 해야하는 값이 있는지 확인
                toJson.put(JsonName(cnt), value);
                cnt++;
            }

            String topic = "application/" + Location(offset);
            log.debug("TOPIC : " + topic);

            ToBroker toBroker = new ToBroker(); //broker에게 전송
            toBroker.connect();
            toBroker.send(toJson, topic);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
    
    @Override
    public void run() {
        while (!Thread.currentThread().interrupted()) {
            try{
                for (int i = start; i <= end; i += step) {
                    load(i);
                }
                Thread.sleep(5000);
            }
            catch(InterruptedException e){
                System.err.println(e.getMessage());
            }
        }
    
    }
}
