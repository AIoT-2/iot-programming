package com.nhnacademy.mtqq.handler;

import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;
import com.nhnacademy.mtqq.Interface.DataSourceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ModbusHandler implements DataSourceHandler {

    private static final Logger log = LoggerFactory.getLogger(ModbusHandler.class);
    private static final String HOST = "192.168.70.203"; // Modbus 서버 IP
    private static final int PORT = Modbus.TCP_PORT; // Modbus TCP 포트
    private static final int SLAVE_ID = 1; // Modbus Slave ID
    private static final int OFFSET = 100; // 읽을 시작 주소
    private static final int QUANTITY = 32; // 읽을 데이터 수

    @Override
    public String handle() {
        ModbusMaster modbusMaster = null;
        String mqttMessage = "";

        try {
            // TCP 파라미터 설정
            TcpParameters tcpParameters = new TcpParameters();
            tcpParameters.setHost(InetAddress.getByName(HOST));
            tcpParameters.setPort(PORT);
            tcpParameters.setKeepAlive(true);

            // Modbus 마스터 생성
            modbusMaster = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
            Modbus.setAutoIncrementTransactionId(true);

            // 연결
            if (!modbusMaster.isConnected()) {
                modbusMaster.connect();
            }

            // Modbus에서 데이터 읽기
            int[] registerValues = modbusMaster.readInputRegisters(SLAVE_ID, OFFSET, QUANTITY);
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("{\"deviceName\":\"modbus_device\", \"data\":[");

            for (int i = 0; i < registerValues.length; i++) {
                messageBuilder.append("{\"address\":").append(OFFSET + i)
                        .append(", \"value\":").append(registerValues[i]).append("}");
                if (i < registerValues.length - 1) {
                    messageBuilder.append(",");
                }
            }
            messageBuilder.append("]}");

            mqttMessage = messageBuilder.toString();
            log.debug("Generated MQTT Message: {}", mqttMessage);

        } catch (ModbusProtocolException | ModbusNumberException | ModbusIOException e) {
            log.error("Error during Modbus communication: {}", e.getMessage());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } finally {
            // 연결 종료
            if (modbusMaster != null && modbusMaster.isConnected()) {
                try {
                    modbusMaster.disconnect();
                } catch (ModbusIOException e) {
                    log.error("Error disconnecting Modbus master: {}", e.getMessage());
                }
            }
        }

        return mqttMessage;
    }
}
