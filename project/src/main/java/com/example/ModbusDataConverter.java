package com.example;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

public class ModbusDataConverter {
    private static final Map<Integer, String> DEVICE_TYPES = new HashMap<>();
    
    static {
        DEVICE_TYPES.put(0, "NOT_USED");
        DEVICE_TYPES.put(1, "1P_R");
        DEVICE_TYPES.put(2, "1P_S");
        DEVICE_TYPES.put(3, "1P_T");
        DEVICE_TYPES.put(4, "3P3W_2CT");
        DEVICE_TYPES.put(5, "3P4W");
        DEVICE_TYPES.put(6, "ZCT");
        DEVICE_TYPES.put(7, "3P3W_3CT");
        DEVICE_TYPES.put(8, "1P3W");
        DEVICE_TYPES.put(9, "ZCT_A");
        DEVICE_TYPES.put(10, "ZCT_B");
        DEVICE_TYPES.put(11, "ZCT_C");
    }

    public String convertToJson(int[] registerValues, String location) {
        Map<String, Object> data = new HashMap<>();
        data.put("location", location);
        
        // type (offset 0)
        int type = registerValues[0];
        data.put("type", DEVICE_TYPES.getOrDefault(type, "UNKNOWN"));
        
        // leakage current (offset 1)
        data.put("leakageCurrent", registerValues[1]);
        
        // current (offset 2-3) UINT32, scale 100
        long current = combineRegisters(registerValues[2], registerValues[3]);
        data.put("current", current / 100.0);
        
        // power W (offset 4-5) INT32, scale 1
        int powerW = combineRegistersToInt(registerValues[4], registerValues[5]);
        data.put("powerW", powerW);
        
        // VAR (offset 6-7) INT32, scale 1
        int var = combineRegistersToInt(registerValues[6], registerValues[7]);
        data.put("powerVAR", var);
        
        // VA (offset 8-9) UINT32, scale 1
        long va = combineRegisters(registerValues[8], registerValues[9]);
        data.put("powerVA", va);
        
        // PF average (offset 10) INT16, scale 100
        data.put("powerFactorAvg", registerValues[10] / 100.0);
        
        // current unbalance (offset 12) UINT16, scale 100
        data.put("currentUnbalance", registerValues[12] / 100.0);
        
        // I THD average (offset 13) UINT16, scale 100
        data.put("currentTHDAvg", registerValues[13] / 100.0);
        
        // Voltage (offset 16-17) UINT32, scale 100
        long voltage = combineRegisters(registerValues[16], registerValues[17]);
        data.put("voltage", voltage / 100.0);
        
        // Current I1 (offset 18-19) UINT32, scale 100
        long currentI1 = combineRegisters(registerValues[18], registerValues[19]);
        data.put("currentI1", currentI1 / 100.0);
        
        return new Gson().toJson(data);
    }

    private long combineRegisters(int reg1, int reg2) {
        return ((long)(reg1 & 0xFFFF) << 16) | (reg2 & 0xFFFF);
    }

    private int combineRegistersToInt(int reg1, int reg2) {
        return (reg1 << 16) | (reg2 & 0xFFFF);
    }
}