package com.example;

import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

public class ConfigReader {

    private Properties properties;

    public ConfigReader(String propertiesFile) {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(propertiesFile)) {
            if (input == null) {
                throw new IOException("프로퍼티 파일을 찾을 수 없습니다.");
            }
            properties.load(input);
        } catch (IOException ex) {
            throw new RuntimeException("프로퍼티 파일 로딩 중 오류 발생: " + ex.getMessage());
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }
}
