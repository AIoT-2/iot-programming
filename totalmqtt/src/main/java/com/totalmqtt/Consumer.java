package com.totalmqtt;

public interface Consumer extends Node { // influxDB
    public void connect();
    public void execute(String jsonData);
}
