package com.nhnacademy.modbus;

public class ModbusException extends Exception {
    public ModbusException(String message) {
        super(message);
    }

    public ModbusException(String message, Throwable cause) {
        super(message, cause);
    }
}
