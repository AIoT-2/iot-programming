package com.nhnacademy.modbus;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Channel {
    @JsonProperty("Offset")
    private int offset;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Type")
    private String type;

    @JsonProperty("Unit")
    private String unit;

    @JsonProperty("Size")
    private int size;

    @JsonProperty("Scale")
    private int scale;

    // getters and setters

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }
}
