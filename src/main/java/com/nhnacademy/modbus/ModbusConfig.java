package com.nhnacademy.modbus;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.msg.request.ReadInputRegistersRequest;
import com.intelligt.modbus.jlibmodbus.msg.response.ReadInputRegistersResponse;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ModbusConfig {
    private ModbusMaster master;

    public ModbusConfig(String host){
        TcpParameters tcpParameters = new TcpParameters();
        try {
            tcpParameters.setHost(InetAddress.getByName(host));
            tcpParameters.setKeepAlive(true);
            tcpParameters.setPort(Modbus.TCP_PORT); // 502
            
            this.master = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
            log.debug("Connected: {}", host);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public ReadInputRegistersResponse readInputRegistersResponse(int slaveId, int channel, int quantity) throws ModbusProtocolException, ModbusIOException{
        ReadInputRegistersRequest request = new ReadInputRegistersRequest();
        try {
            request.setServerAddress(slaveId);
            request.setStartAddress(channel);
            request.setQuantity(quantity);
        } catch (ModbusNumberException e) {
            e.printStackTrace();
        }
        
        return (ReadInputRegistersResponse) master.processRequest(request);
    }

    public ModbusMaster getMaster(){
        return master;
    }

    public void close(){
        if(master.isConnected()){
            try {
                master.disconnect();
            } catch (ModbusIOException e) {
                e.printStackTrace();
            }
        }
    }
}
