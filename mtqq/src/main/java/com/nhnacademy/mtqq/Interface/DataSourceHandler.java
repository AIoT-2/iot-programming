package com.nhnacademy.mtqq.Interface;

public interface DataSourceHandler {
    //데이터를 MQTT 메시지로 변환
    String transformToMqtt();
    //처리 가능한 소스인지 확인
    boolean canHandle(String sourceType);
}
