package com.iot.mqtt;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Mqtt_transform {
    public static String extractField(String topic) {
        Pattern pattern = Pattern.compile("/n/([^/]+)");
        Matcher matcher = pattern.matcher(topic);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    public static String extractMeasurement(String topic) {
        Pattern pattern = Pattern.compile("/p/([^/]+)");
        Matcher matcher = pattern.matcher(topic);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    public static String extractValue(String topic) {
        Pattern pattern = Pattern.compile("/e/([^/]+)");
        Matcher matcher = pattern.matcher(topic);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

}
