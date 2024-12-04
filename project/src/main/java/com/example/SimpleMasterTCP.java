package com.example;

import java.net.InetAddress;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.msg.request.ReadInputRegistersRequest;
import com.intelligt.modbus.jlibmodbus.msg.response.ReadInputRegistersResponse;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;

/*
 * Copyright (C) 2016 "Invertor" Factory", JSC
 * All rights reserved
 *
 * This file is part of JLibModbus.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse
 * or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Authors: Vladislav Y. Kochedykov, software engineer.
 * email: vladislav.kochedykov@gmail.com
 */
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
            tcpParameters.setKeepAlive(true); // tcp 유지 
            tcpParameters.setPort(Modbus.TCP_PORT); // 502번 포트 (고정값)

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

                // at next string we receive ten registers from a slave with id of 1 at offset
                // of 0.
                int[] registerValues = m.readInputRegisters(slaveId, offset, quantity); // offset 100  -> doc/ch02/section_03 채널 정보 참고 -- > 1번 채널이고 위치는 캠퍼스
                // 캠퍼스 위치에 있는 장치의 정보를 가져오겠다
                // quantity 32는 데이터 중 32개를 가져온다는 의미
                // 이 데이터가 Channel별 정보를 의미함 
                // Offset이 100이든 200이든 300이든 어쨌든 끝자리가 0이기에 채널별정보 0을 해석해야한다
                // Offset의 2와 3이 합쳐져서 2가 32비트를 가짐  2번의 경우 Scale 100인데 100만큼 나누라는 의미
                // 이런 식으로 정리 후 JSON으로 만들어야햄 
                // TOPIC은 이름 JSON 데이터 만들고 -> examples/mqtt_client 보면 메세지 발행 이라는 주석 부분 보기
                // "hello"는 토픽인데 뒤에 부분 message가 데이터 
                // 여기에 토픽: 위치 message:json을 만들어서 publish하면 브로커로 보내주는데
                // 여기까지가 modbus를 mqtt로 만들고 브로커로 보내는 과정 
                
                
                // 설정 레지스터 registerValues
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

                // Holidng = 지정 레지스터
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