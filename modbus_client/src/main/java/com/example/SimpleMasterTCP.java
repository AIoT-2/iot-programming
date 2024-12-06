package com.example;

import org.json.JSONArray;
import org.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.math.BigInteger;

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
    private final Logger log = LoggerFactory.getLogger(getClass());


    static public void main(String[] args) {

        JSONArray addressArray = null;
        JSONArray channelAddressArray = null;
        JSONArray channelInfoArray = null;  

        try {
            //센서로 들어오는 통합 정보
            String addressContent = new String(Files.readAllBytes(Paths.get("modbus_client/src/main/java/com/example/AddressMap.json")));
            String channelAddressContent = new String(Files.readAllBytes(Paths.get("modbus_client/src/main/java/com/example/ChannelAddress.json")));
            String channelInfoContent = new String(Files.readAllBytes(Paths.get("modbus_client/src/main/java/com/example/ChannelInfo.json")));

            addressArray = new JSONArray(addressContent);
            channelAddressArray = new JSONArray(channelAddressContent);
            channelInfoArray = new JSONArray(channelInfoContent);


        } catch (IOException e) {
            e.printStackTrace();
        }
        

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
                tcpParameters.setHost(InetAddress.getByName("192.168.70.203"));
                tcpParameters.setKeepAlive(true);
                tcpParameters.setPort(Modbus.TCP_PORT);
        
                ModbusMaster m = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
                Modbus.setAutoIncrementTransactionId(true);
        
        
                try {
                    if (!m.isConnected()) {
                        m.connect();
                    }
        
                    // channelAddressArray에서 address 값을 동적으로 가져오기
                    // offset 값이 100, 200, ..., 24000으로 증가
                    for (int i = 0; i < channelAddressArray.length(); i++) {
                        JSONObject channelAddressObject = channelAddressArray.getJSONObject(i);
                        
                        int baseOffset = channelAddressObject.getInt("address"); // JSON에서 "address" 값을 가져옴
                        int quantity = 32; // 한 번에 32개의 레지스터 읽기
                        

                        try {
                            // 요청 객체 생성
                            ReadInputRegistersRequest request = new ReadInputRegistersRequest();
                            request.setServerAddress(1); // slaveId
                            request.setStartAddress(baseOffset); // 시작 주소
                            request.setQuantity(quantity); // 읽을 레지스터 수

                            // 요청 실행
                            ReadInputRegistersResponse response = (ReadInputRegistersResponse) m.processRequest(request);

                            // 가져온 레지스터 출력
                            int loopLimit = Math.min(response.getHoldingRegisters().getQuantity(), channelInfoArray.length());
                            // 중복되는 채널 정보는 제거하고 출력
                            for (int k = 0; k < loopLimit; k++) {
                                JSONObject channelInfoObject = channelInfoArray.getJSONObject(k);
                                int size = channelInfoObject.getInt("size");
                                BigInteger scale = channelInfoObject.get("scale");
                                if(scale == null){
                                    System.out.println("Value is null");
                                    continue;
                                }
                                System.out.println("Address: " + (baseOffset + k) + ", Value: " + response.getHoldingRegisters().get(k));
                                if (size == 1 && (k + 1) < response.getHoldingRegisters().getQuantity()) {
                                    int value = (response.getHoldingRegisters().get(k) << 8) | response.getHoldingRegisters().get(k + 1);

                                    System.out.println("16-bit Value: " + value / scale);
                                } else if (size == 2 && (k + 1) < response.getHoldingRegisters().getQuantity()) {
                                    int value = (response.getHoldingRegisters().get(k) << 16) | response.getHoldingRegisters().get(k + 1);
                                    System.out.println("32-bit Value: " + value / scale);
                                }
                            }


                        } catch (ModbusProtocolException | ModbusNumberException | ModbusIOException e) {
                            e.printStackTrace();
                        }
                    }

                } 
                finally {
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
