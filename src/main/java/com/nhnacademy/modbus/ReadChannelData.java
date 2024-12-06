package com.nhnacademy.modbus;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelligt.modbus.jlibmodbus.msg.response.ReadInputRegistersResponse;

public class ReadChannelData {
    private static ObjectMapper mapper = new ObjectMapper();
    private static Map<Integer, Map<String, Object>> offsetMap;
    
    
    public static Map<Integer, String> loadChannelData(String filePath) throws IOException{
        List<Map<String, Object>> list = mapper.readValue(new File(filePath), List.class);
        Map<Integer, String> channelMap = new HashMap<>();
        for (Map<String, Object> item : list) {
            int address = (Integer) item.get("address");
            String spot = (String) item.get("spot");
            channelMap.put(address, spot);
        }
        return channelMap;
    }

    public static Map<Integer, Map<String, Object>> loadOffsetData(String filePath) throws IOException{
        List<Map<String, Object>> list = mapper.readValue(new File(filePath), List.class);
        offsetMap = new HashMap<>();

        for(Map<String, Object> item : list){
            int offset = (Integer) item.get("offset");
            item.remove("offset");
            offsetMap.put(offset, item);
        }
        return offsetMap;
    }

    public static Map<String, Object> getDataMap(ReadInputRegistersResponse response, int startAddress) {
        Map<String, Object> dataMap = new HashMap<>();
        for (int offset = 0; offset < response.getHoldingRegisters().getQuantity(); offset++) {
            int registerValue;
            try {
                registerValue = response.getHoldingRegisters().get(offset);

                String addressName = (String) offsetMap.get(offset).get("name");
                int scale = (int) offsetMap.get(offset).get("scale");

                if ((int) offsetMap.get(offset).get("size") == 2) {
                    int firstRegisterValue = response.getHoldingRegisters().get(offset);
                    int secondRegisterValue = response.getHoldingRegisters().get(offset + 1);
                    registerValue = (firstRegisterValue << 16) | secondRegisterValue;
                    offset++;
                }

                double realRegisterValue = registerValue / scale;
                dataMap.put(addressName, checkDouble(realRegisterValue) ? (int) realRegisterValue : (double) realRegisterValue);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return dataMap;
    }

    public static boolean checkDouble(double realRegisterValue) {
        return realRegisterValue == Math.floor(realRegisterValue);
    }
    
}
