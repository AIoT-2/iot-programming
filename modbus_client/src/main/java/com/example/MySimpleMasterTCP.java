package com.example;

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

public class MySimpleMasterTCP {
    static public void main(String[] args){
        Modbus.log().addHandler(new Handler(){
            @Override
            public void publish(LogRecord record){
                System.out.println(record.getLevel().getName() + ": " + record.getMessage());
            }

            @Override
            public void flush(){
                //do nothing
            }

            @Override
            public void close() throws SecurityException {
                //do nothing
            }
        });
        Modbus.setLogLevel(Modbus.LogLevel.LEVEL_DEBUG);

        try{
            TcpParameters tcpParameters = new TcpParameters();
            
            tcpParameters.setHost(InetAddress.getByName("192.168.70.204"));
            tcpParameters.setKeepAlive(true);
            tcpParameters.setPort(Modbus.TCP_PORT);
        }
    }
}
