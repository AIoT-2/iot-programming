package com.nhnacademy.mtqq.mtqq;

import com.nhnacademy.mtqq.Interface.DataSourceHandler;
import com.nhnacademy.mtqq.handler.ModbusHandler;
import com.nhnacademy.mtqq.handler.TCPHandler;

import java.util.ArrayList;
import java.util.List;

public class TransForMqtt {
    private final List<DataSourceHandler> handlers;

    public TransForMqtt(){
        handlers = new ArrayList<>();
        handlers.add(new ModbusHandler());
        handlers.add(new TCPHandler());
    }

    public String process(String sourceType){
        for(DataSourceHandler handler: handlers){
            if(handler.canHandle((sourceType))){
                return handler.transformToMqtt();
            }
        }
        throw new IllegalArgumentException("Unsupported source type: " + sourceType);
    }
}
