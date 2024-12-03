package com.example.mqtt_mqtt;

public class SensorData {
    private String topic; // MQTT 메시지의 topic
    private String valuename; // topic의 마지막 부분
    private double value; // 해당 topic의 value 값
    private long time; // 메시지의 time 값

    // Getter와 Setter
    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getValuename() {
        return valuename;
    }

    public void setValuename(String valuename) {
        this.valuename = valuename;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "SensorData{" +
                "topic='" + topic +
                ", " + valuename + "=" + value +
                ", time=" + time +
                '}';
    }
}