package com.nhnacademy.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class ConfigLoader {
    private ConfigLoader() {}

    public static AppConfig loadConfig(String filePath) {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        try {
            // classpath에서 config.yml을 읽기 위해 경로를 수정
            InputStream inputStream =
                    ConfigLoader.class.getClassLoader().getResourceAsStream(filePath);
            if (inputStream == null) {
                throw new FileNotFoundException("config.yml not found in classpath");
            }
            return objectMapper.readValue(inputStream, AppConfig.class);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error loading config", e);
        }
    }
}
