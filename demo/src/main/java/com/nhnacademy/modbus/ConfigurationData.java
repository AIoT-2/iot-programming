package com.nhnacademy.modbus;

import java.util.HashMap;
import java.util.Map;

import com.nhnacademy.settings.DemoSetting;

public class ConfigurationData {
    // addr, name, scale, type를 넣습니다.
    public static Map<Integer, String[]> addressMapName() {
        Map<Integer, String[]> map = new HashMap<>();

        map.put(0, new String[] { "operation Heartbit", "1", "16" });
        map.put(1, new String[] { "temperature", "10", "16" });
        map.put(2, new String[] { "frequency", "100", "16" });
        map.put(3, new String[] { "program version", "1", "16" });
        map.put(4, new String[] { "present CO2 use(month)", "10", "16" });
        map.put(6, new String[] { "operation Heartbit", "1", "16" });
        map.put(7, new String[] { "temperature 1", "10", "16" });
        map.put(8, new String[] { "frequency", "100", "16" });
        map.put(9, new String[] { "program version", "1", "16" });
        map.put(10, new String[] { "present CO2 use(month)", "10", "16" });
        map.put(12, new String[] { "V123(LN) average", "100", "32" });
        map.put(13, new String[] { "V123(LL) average", "100", "32" });
        map.put(14, new String[] { "V123(LN) unbalance", "100", "16" });
        map.put(15, new String[] { "V123(LL) unbalance", "100", "16" });
        map.put(16, new String[] { "V1", "100", "32" });
        map.put(17, new String[] { "V12", "100", "32" });
        map.put(18, new String[] { "V1 unbalance", "100", "16" });
        map.put(19, new String[] { "V12 unbalance", "100", "16" });
        map.put(20, new String[] { "V2", "100", "32" });
        map.put(21, new String[] { "V23", "100", "32" });
        map.put(22, new String[] { "V2 unbalance", "100", "16" });
        map.put(23, new String[] { "V23 unbalance", "100", "16" });
        map.put(24, new String[] { "V3", "100", "32" });
        map.put(25, new String[] { "V31", "100", "32" });
        map.put(26, new String[] { "V3 unbalance", "100", "16" });
        map.put(27, new String[] { "V31 unbalance", "100", "16" });
        map.put(28, new String[] { "V1 THD", "100", "16" });
        map.put(29, new String[] { "V2 THD", "100", "16" });
        map.put(30, new String[] { "V3 THD", "100", "16" });
        map.put(50, new String[] { "" });

        return map;
    }

    // offset에 해당하는 이름을 얻는 메서드
    public static Map<Integer, String> topicMapName() {
        Map<Integer, String> topicMap = new HashMap<>();

        String topic = DemoSetting.MODBUS_TOPIC;

        topicMap.put(100, topic + "/캠퍼스");
        topicMap.put(200, topic + "/캠퍼스1");
        topicMap.put(300, topic + "/전등1");
        topicMap.put(400, topic + "/전등2");
        topicMap.put(500, topic + "/전등3");
        topicMap.put(600, topic + "/강의실b_바닥");
        topicMap.put(700, topic + "/강의실a_바닥1");
        topicMap.put(800, topic + "/강의실a_바닥2");
        topicMap.put(900, topic + "/프로젝터1");
        topicMap.put(1000, topic + "/프로젝터2");
        topicMap.put(1100, topic + "/회의실");
        topicMap.put(1200, topic + "/서버실");
        topicMap.put(1300, topic + "/간판");
        topicMap.put(1400, topic + "/페어룸");
        topicMap.put(1500, topic + "/사무실_전열1");
        topicMap.put(1600, topic + "/사무실_전열2");
        topicMap.put(1700, topic + "/사무실_복사기");
        topicMap.put(1800, topic + "/빌트인_전열");
        topicMap.put(1900, topic + "/사무실_전열3");
        topicMap.put(2000, topic + "/정수기");
        topicMap.put(2100, topic + "/하이브_전열");
        topicMap.put(2200, topic + "/바텐_전열");
        topicMap.put(2300, topic + "/S_P");
        topicMap.put(2400, topic + "/공조기");
        topicMap.put(2500, topic + "/AC");

        return topicMap;
    }

    public static int applyScale(int address, int value) {
        Map<Integer, String[]> registerMap = addressMapName();

        String[] registerInfo = registerMap.get(address);

        int scale = Integer.parseInt(registerInfo[1]);

        return value / scale;
    }

    private ConfigurationData() {
    }
}
