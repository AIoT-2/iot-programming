package com.nhnacademy.mtqq.Interface;

import com.nhnacademy.mtqq.exception.DataSourceHandlerException;

import java.util.Map;

public interface TransForMqtt {
    Map<String, Object> transFromMqttMessage(Map<String, Map<Integer, Double>> locationData);
}

