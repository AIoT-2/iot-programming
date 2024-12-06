package com.totalmqtt;

public interface Producer extends Node{ //modbus, mqtt
    public void connect();
    public void execute(int offset);
}
