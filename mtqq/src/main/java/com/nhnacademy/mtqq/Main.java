package com.nhnacademy.mtqq;

import com.nhnacademy.mtqq.mtqq.MqttToInfluxDB;
import com.nhnacademy.mtqq.mtqq.TransForMqtt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args){
        TransForMqtt transForMqtt = new TransForMqtt();

        try {
            String sourceType = "Tcp";
            String mqttMessage = transForMqtt.process(sourceType);
            log.info("mqttMessage: {}", mqttMessage);

            MqttToInfluxDB mqttToInfluxDB = new MqttToInfluxDB();
            mqttToInfluxDB.run(mqttMessage);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
