package com.nhnacademy.config;

public class AppConfig {
    private MqttConfig mqtt;
    private InfluxdbConfig influxdb;
    private ModbusConfig modbus;

    public MqttConfig getMqtt() {
        return mqtt;
    }

    public void setMqtt(MqttConfig mqtt) {
        this.mqtt = mqtt;
    }

    public InfluxdbConfig getInfluxdb() {
        return influxdb;
    }

    public void setInfluxdb(InfluxdbConfig influxdb) {
        this.influxdb = influxdb;
    }

    public ModbusConfig getModbus() {
        return modbus;
    }

    public void setModbus(ModbusConfig modbus) {
        this.modbus = modbus;
    }
}
