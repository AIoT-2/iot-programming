package com.example.modbus;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Type;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;


@Slf4j
public class MyModbus {
    private static int slaveId;
    private static int offset;
    private static int quantity;
    private static ReadInputRegistersRequest request = new ReadInputRegistersRequest();
    private static ReadInputRegistersResponse response = new ReadInputRegistersResponse();
    private static Map<Integer, Object> addressMap = new HashMap<>();
    private static Map<Integer, Object> infoMap = new HashMap<>();
    private static Map<Map<String, Object>, List<Map<String, Object>>> resultMap = new HashMap<>();

    private static Reader reader;
    private static JSONParser parser = new JSONParser();

    private static String addressPath = "/home/nhnacademy/Code/mqtt_example/mqtt/src/main/java/com/example/modbus/Channel_Address.json";
    private static String infoPath = "/home/nhnacademy/Code/mqtt_example/mqtt/src/main/java/com/example/modbus/Channel_Info.json";
        private static final String ERRMESSAGE = "ErrorMessage: {}";

    static public void main(String[] args) {

        createAddressMap();
        
        Modbus.log().addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                log.info("{}: {}", record.getLevel().getName(), record.getMessage());
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

            slaveId = 1;
            offset = 100;
            quantity = 63;

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
                
                request.setServerAddress(slaveId);
                request.setStartAddress(offset);
                request.setQuantity(quantity);
                request.setTransactionId(1);
                
                // you can get either int[] containing register values or byte[] containing raw
                // bytes.
                for (int i = 0; i < response.getHoldingRegisters().getQuantity(); i++) {
                    System.out.println("Address: " + (offset + i) + ", Value: "
                            + response.getHoldingRegisters().get(i));
                }
                int value = (response.getHoldingRegisters().get(16) << 16) | response.getHoldingRegisters().get(17);

                System.out.println("V1 : " + value / 100);
            } catch (ModbusProtocolException | ModbusNumberException | ModbusIOException e) {
                log.debug(addressPath);
            } finally {
                try {
                    m.disconnect();
                } catch (ModbusIOException e) {
                    log.debug(ERRMESSAGE, e.getMessage());
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.debug(ERRMESSAGE, e.getMessage());
        }
    }

    private static void createAddressMap() {
        try{
            reader = new FileReader(addressPath);

            JSONArray jsonArray = (JSONArray) parser.parse(reader);

            // JSON 배열 순회
            for (Object object : jsonArray) {
                JSONObject jsonObject = (JSONObject) object;
                addressMap.put(((Long) jsonObject.get("channel")).intValue(), (String)jsonObject.get("location"));
            }
            log.debug("addressMap \n{}", addressMap);
        } catch (Exception e) {
            log.debug(ERRMESSAGE, e.getMessage());
        }
    }

    private static void createInfoMap() {
        try{
            reader = new FileReader(infoPath);

            JSONArray jsonArray = (JSONArray) parser.parse(reader);

            // JSON 배열 순회
            for (Object object : jsonArray) {
                JSONObject jsonObject = (JSONObject) object;
                addressMap.put(((Long) jsonObject.get("offset")).intValue(), (String)jsonObject.get("name"));
            }
            log.debug("addressMap \n{}", addressMap);
        } catch (Exception e) {
            log.debug(ERRMESSAGE, e.getMessage());
        }
    }

    // private void getData() {
    //     try {
    //         Map<Map<String, Object>, List<Map<String, Object>>> chDataMap = resultMap.get(offset);
            
    //         Map<String, Object> inputDataMap = new HashMap<>();

    //         double value;

    //         for (int i = 0; i <= quantity; i++) {
    //             if(resultMap.get)
    //             value = response.getHoldingRegisters().get(i);
    //         }
    //     } catch (Exception e) {
    //         log.debug(ERRMESSAGE, e.getMessage());
    //     }
    // }


}
