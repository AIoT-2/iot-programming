package com.nhnacademy;

// 가공된 메시지 구조를 위한 클래스
public class ProcessedMessage {
    private final double temperature;
    private final long timestamp;
    private final String tag;

    public ProcessedMessage(double temperature, long timestamp, String tag) {
        this.temperature = temperature;
        this.timestamp = timestamp;
        this.tag = tag;
    }

    public double getTemperature() {
        return temperature;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getTag() {
        return tag;
    }
}
