package com.nhnacademy.modbus;

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
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class SimpleMasterTCP {

    static public void main(String[] args) {

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
            int offset = 0;
            int quantity = 32;

            try {
                // since 1.2.8
                if (!m.isConnected()) {
                    m.connect();
                }

                // at next string we receive ten registers from a slave with id of 1 at offset
                // of 0.
                int[] registerValues = m.readInputRegisters(slaveId, offset, quantity);

                for (int i = 0; i < registerValues.length; i++) {
                    System.out.println("Address: " + (offset + i) + ", Value: " + registerValues[i]);
                }
                // also since 1.2.8.4 you can create your own request and process it with the
                // master
                ReadInputRegistersRequest request = new ReadInputRegistersRequest();
                request.setServerAddress(slaveId);
                request.setStartAddress(offset);
                request.setQuantity(quantity);
                request.setTransactionId(1);
                ReadInputRegistersResponse response = (ReadInputRegistersResponse) m.processRequest(request);
                // you can get either int[] containing register values or byte[] containing raw
                // bytes.
                for (int i = 0; i < response.getHoldingRegisters().getQuantity(); i++) {
                    System.out.println("Address: " + (offset + i) + ", Value: "
                            + response.getHoldingRegisters().get(i));
                }

                // 두개의 레지스터 값을 합해서 int32 데이터 하나를 만듭니다.
                // 라이브러리는 little-endian기준으로 되어 있지만, 실제 장비는 little-endian과 big-endian 둘다 사용됩니다.
                // 따라서, 실험 환경이 big-endian이므로 아래와 같이 만들어 줍니다.
                int value = (response.getHoldingRegisters().get(16) << 16) | response.getHoldingRegisters().get(17);

                // 자료를 참고하여 스케일을 적용합니다.
                System.out.println("V1 : " + value / 100);
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