package com.nhnacademy.nhnacademy.result;

import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ModbusToMQTT {
    public static void main(String[] args) {
        // Modbus TCP 설정
        String modbusIP = "192.168.70.203";
        int modbusPort = Modbus.TCP_PORT; // 기본 포트 502
        int slaveId = 1; // 슬레이브 ID
        int offset = 100; // 데이터 시작 주소
        int quantity = 72; // 읽을 레지스터 개수 (필요에 따라 조정)

        // MQTT 설정
        String broker = "tcp://127.0.0.1:1883"; // 브로커 주소
        String topic = "sensor/data"; // MQTT 토픽
        String clientId = "ModbusToMQTTClient";

        try {
            // MQTT 클라이언트 생성
            MqttClient mqttClient = new MqttClient(broker, clientId);
            mqttClient.connect();

            // Modbus 설정
            TcpParameters tcpParameters = new TcpParameters();
            tcpParameters.setHost(InetAddress.getByName(modbusIP));
            tcpParameters.setKeepAlive(true);
            tcpParameters.setPort(modbusPort);

            ModbusMaster modbusMaster = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
            Modbus.setAutoIncrementTransactionId(true);

            if (!modbusMaster.isConnected()) {
                modbusMaster.connect();
            }

            // Modbus 데이터 읽기
            int[] registers = modbusMaster.readInputRegisters(slaveId, offset, quantity);
            // JSON 객체 생성
            JSONObject jsonData = new JSONObject();


            // 16비트 및 32비트 데이터를 처리
            jsonData.put("type", registers[0]); // Offset 0
            jsonData.put("leakageCurrent", registers[1] / 10.0); // Offset 1 (mA)

            jsonData.put("current", combineRegisters(registers, 2) / 100.0); // Offset 2-3 (A)
            jsonData.put("W", combineRegisters(registers, 4)); // Offset 4-5 (W)
            jsonData.put("VAR", combineRegisters(registers, 6)); // Offset 6-7 (VAR)
            jsonData.put("VA", combineRegisters(registers, 8)); // Offset 8-9 (VA)

            jsonData.put("pfAverage", registers[10] / 100.0); // Offset 10 (%)
            jsonData.put("currentUnbalance", registers[12] / 100.0); // Offset 12 (%)
            jsonData.put("ITHDAverage", registers[13] / 100.0); // Offset 13 (%)
            jsonData.put("IGR", registers[14] / 10.0); // Offset 14 (mA)
            jsonData.put("IGC", registers[15] / 10.0); // Offset 15 (mA)

            jsonData.put("V1", combineRegisters(registers, 16) / 100.0); // Offset 16-17 (V)
            jsonData.put("I1", combineRegisters(registers, 18) / 100.0); // Offset 18-19 (A)
            jsonData.put("W2", combineRegisters(registers, 20)); // Offset 20-21 (W)
            jsonData.put("VAR2", combineRegisters(registers, 22)); // Offset 22-23 (VAR)
            jsonData.put("VA2", combineRegisters(registers, 24)); // Offset 24-25 (VA)

            jsonData.put("voltUnbalance", registers[26] / 100.0); // Offset 26 (%)
            jsonData.put("currentUnbalance2", registers[27] / 100.0); // Offset 27 (%)
            jsonData.put("phase", registers[28] / 100.0); // Offset 28 (°)
            jsonData.put("powerFactor", registers[29] / 100.0); // Offset 29 (%)
            jsonData.put("I1THD", registers[30] / 100.0); // Offset 30 (%)

            jsonData.put("V2", combineRegisters(registers, 32) / 100.0); // Offset 32-33 (V)
            jsonData.put("I2", combineRegisters(registers, 34) / 100.0); // Offset 34-35 (A)
            jsonData.put("W3", combineRegisters(registers, 36)); // Offset 36-37 (W)
            jsonData.put("VAR3", combineRegisters(registers, 38)); // Offset 38-39 (VAR)
            jsonData.put("VA3", combineRegisters(registers, 40)); // Offset 40-41 (VA)

            jsonData.put("voltUnbalance2", registers[42] / 100.0); // Offset 42 (%)
            jsonData.put("currentUnbalance3", registers[43] / 100.0); // Offset 43 (%)
            jsonData.put("phase2", registers[44] / 100.0); // Offset 44 (°)
            jsonData.put("powerFactor2", registers[45] / 100.0); // Offset 45 (%)
            jsonData.put("I2THD", registers[46] / 100.0); // Offset 46 (%)

            jsonData.put("V3", combineRegisters(registers, 48) / 100.0); // Offset 48-49 (V)
            jsonData.put("I3", combineRegisters(registers, 70) / 100.0); // Offset 70-71 (A)
            jsonData.put("W4", combineRegisters(registers, 52)); // Offset 52-53 (W)
            jsonData.put("VAR4", combineRegisters(registers, 54)); // Offset 54-55 (VAR)
            jsonData.put("VA4", combineRegisters(registers, 56)); // Offset 56-57 (VA)

            jsonData.put("voltUnbalance3", registers[58] / 100.0); // Offset 58 (%)
            jsonData.put("currentUnbalance4", registers[59] / 100.0); // Offset 59 (%)
            jsonData.put("phase3", registers[60] / 100.0); // Offset 60 (°)
            jsonData.put("powerFactor3", registers[61] / 100.0); // Offset 61 (%)
            jsonData.put("I3THD", registers[62] / 100.0); // Offset 62 (%)
            // 새로운 JSON 객체 생성 (외부에 "data"와 "deviceName" 추가)
            JSONObject finalData = new JSONObject();
            finalData.put("data", jsonData); // data 속성 안에 기존 데이터를 포함
            finalData.put("deviceName", "캠퍼스");

            MqttMessage message = new MqttMessage(finalData.toString().getBytes());
            message.setQos(0); // QoS 설정 (0, 1, 2 중 선택)
            mqttClient.publish(topic, message);


            System.out.println("Published data: " + finalData);

            // 연결 종료
            mqttClient.disconnect();
            modbusMaster.disconnect();

        } catch (ModbusProtocolException | ModbusNumberException | ModbusIOException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
    static long combineRegisters(int[] registers, int offset) {
        return ((long) registers[offset] << 16) | (registers[offset + 1] & 0xFFFF);
    }


}
