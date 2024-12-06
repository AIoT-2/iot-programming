package com.nhnacademy.utils;

import java.util.HashMap;
import java.util.Map;

public class TopicParser {
    private TopicParser() {
        throw new UnsupportedOperationException("이 클래스는 인스턴스화할 수 없습니다.");
    }

    public static Map<String, String> parse(String topic) {
        String[] topicParts = topic.split("/");
        int numOfTag = topicParts.length / 2;
        Map<String, String> tags = new HashMap<>();

        for (int i = 0; i < numOfTag; i++) {
            String key = "unknown";

            switch (topicParts[2 * i + 1]) {
                case "s":
                    key = "site";
                    break;
                case "b":
                    key = "branch";
                    break;
                case "p":
                    key = "place";
                    break;
                case "e":
                    key = "element";
                    break;
                case "d":
                    key = "device";
                    break;
                case "sp":
                    key = "spot";
                    break;
                case "n":
                    key = "name";
                    break;
                case "g":
                    key = "gateway";
                    break;
                default:
                    break;
            }
            String value = topicParts[2 * i + 2];

            tags.put(key, value);
        }

        return tags;
    }
}
