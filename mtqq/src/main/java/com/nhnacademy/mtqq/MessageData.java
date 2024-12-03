package com.nhnacademy.mtqq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageData {

    private String topic;
    private String message;
    private String deviceName;
    private double voltage;
    private double current;
    private double activePower;
    private double reactivePower;
    private double tapparentPower;
    private double phase;
    private double powerFactor;

    public void setDeviceName(String deviceName){
        this.deviceName = deviceName;
    }

    public String getDeviceName(){
        return deviceName;
    }

    public void setTopic(String topic){
        this.topic =  topic;
    }

    public String getTopic(){
        return topic;
    }

    public void setMessage(String message){
        this.message = message;
    }

    public String getMessage(){
        return message;
    }

    public void setVoltage(double voltage){
        this.voltage = voltage;
    }

    public double getVoltage(){
        return voltage;
    }

    public void setCurrent(double current){
        this.current = current;
    }

    public double getCurrent(){
        return current;
    }

    public void setActivePower(double activePower){
        this.activePower = activePower;
    }

    public double getActivePower(){
        return activePower;
    }

    public void setReactivePower(double reactivePower){
        this.reactivePower = reactivePower;
    }

    public double getReactivePower(){
        return reactivePower;
    }

    public void setTapparentPower(double tapparentPower){
        this.tapparentPower = tapparentPower;
    }

    public double getTapparentPower(){
        return tapparentPower;
    }

    public void setPhase(double phase){
        this.phase = phase;
    }

    public double getPhase(){
        return phase;
    }

    public void setPowerFactor(double powerFactor){
        this.powerFactor = powerFactor;
    }

    public double getPowerFactor(){
        return powerFactor;
    }
}
