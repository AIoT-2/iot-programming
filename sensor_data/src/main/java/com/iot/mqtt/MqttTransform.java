package com.iot.mqtt;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MqttTransform {
    public static String extractName(String topic) {
        Pattern pattern = Pattern.compile("/n/([^/]+)");
        Matcher matcher = pattern.matcher(topic);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    public static String extractPlace(String topic) {
        Pattern pattern = Pattern.compile("/p/([^/]+)");
        Matcher matcher = pattern.matcher(topic);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    public static String extractElement(String topic) {
        Pattern pattern = Pattern.compile("/e/([^/]+)");
        Matcher matcher = pattern.matcher(topic);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }
}
