package com.nhnacademy.mtqq.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;
import com.nhnacademy.mtqq.Interface.DataSourceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

public class ModbusHandler implements DataSourceHandler {
    private static final Logger log = LoggerFactory.getLogger(ModbusHandler.class);
    @Override
    public String transformToMqtt() {
        try{
            TcpParameters tcpParameters = new TcpParameters();
            tcpParameters.setHost(InetAddress.getByName("192.168.70.204"));
            tcpParameters.setPort(502);

            ModbusMaster modbusMaster = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
            modbusMaster.connect();

            int slaveId = 1, offset = 100, quantity = 10;
            int[] values = modbusMaster.readInputRegisters(slaveId, offset, quantity);

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode jsonNode = objectMapper.createObjectNode();
            jsonNode.put("source", "Modbus");
            for(int i = 0; i < values.length; i++){
                jsonNode.put("register_" + (offset + i), values[i]);
            }

            modbusMaster.disconnect();
            return objectMapper.writeValueAsString(jsonNode);
        }catch(Exception e){
            log.debug("Exception Error: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public boolean canHandle(String sourceType) {
        return "Modbus".equalsIgnoreCase(sourceType);
    }
}
