package com.nhnacademy.modbus;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ModbusToMqtt implements Runnable {
    // MQTT 설정
    static final String DEFAULT_BROKER = "tcp://127.0.0.1:1883"; // 브로커 주소
    static final String DEFAULT_TOPIC = "sensor/data"; // MQTT 토픽

    private final String broker;
    private final String topic;


    // Modbus 설정
    static final String DEFAULT_TCP_IP = "192.168.70.203";
    static final int DEFAULT_SLAVE_ID = 1;

    private final List<Channel> channels;
    private final String tcpIp;
    private final int slaveId; // 슬레이브 ID

    public ModbusToMqtt() {
        this(DEFAULT_BROKER, DEFAULT_TOPIC, DEFAULT_SLAVE_ID, DEFAULT_TCP_IP);
    }

    public ModbusToMqtt(String broker, String topic, int slaveId, String tcpIP) {
        this.broker = broker;
        this.topic = topic;
        this.slaveId = slaveId;
        this.tcpIp = tcpIP;
        this.channels = loadChannelsFromResource();
    }

    // json 파일로 부터 channel 정보 파싱
    private List<Channel> loadChannelsFromResource() {
        String resourcePath = "json/InformationByChannel.json";
        ObjectMapper objectMapper = new ObjectMapper();
        InputStream jsonInput = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (jsonInput == null) {
            throw new IllegalArgumentException("JSON file not found in resources: " + resourcePath);
        }
        try {
            // JSON 구조에서 "Channels" 배열을 추출
            JsonNode rootNode = objectMapper.readTree(jsonInput);
            JsonNode channelsNode = rootNode.get("Channels"); // "Channels"가 배열로 존재
            if (channelsNode == null || !channelsNode.isArray()) {
                throw new IllegalArgumentException("Channels array not found or malformed in JSON file.");
            }
            return objectMapper.readValue(channelsNode.toString(), new TypeReference<List<Channel>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to load JSON file: " + resourcePath, e);
        }
    }



    public TcpParameters tcpParametersConstructor() {
        TcpParameters tcpParameters;
        try {
            tcpParameters = new TcpParameters();
            tcpParameters.setHost(InetAddress.getByName(tcpIp));
            tcpParameters.setPort(Modbus.TCP_PORT);

        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        return tcpParameters;
    }

    public ModbusMaster ModbusMasterConstructor(TcpParameters tcpParameters) {
        ModbusMaster modbusMaster;
        modbusMaster = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
        Modbus.setAutoIncrementTransactionId(true);
        return modbusMaster;
    }


    @Override
    public void run() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                ModbusMaster modbusMaster = ModbusMasterConstructor(tcpParametersConstructor());

                // 필요한 최대 레지스터 계산
                int maxOffset = getMaxOffset();

                // Modbus 데이터를 한 번 읽어서 저장
                int[] registers = modbusMaster.readInputRegisters(slaveId, 0, maxOffset + 1); // 0부터 maxOffset까지 읽음

                //MQTT 메시지 생성
                Map<String, Object> data = new HashMap<>();
                Map<String, String> tags = new HashMap<>();

                // 레지스터 순회 데이터 입력
                putData(data, registers);

                // 태그 정보 추가
                tags.put("deviceId", "Campus");
                tags.put("location", "캠퍼스");

                //Mqtt Message 전송
                sendMqttMessage(data, tags);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    private void putData(Map<String, Object> data, int[] registers) {
        for (Channel channel : channels) {
            Object value = processChannel(channel, registers);
            if (value != null) {
                data.put(channel.getName(), value);
            }
        }
    }

    private int getMaxOffset() {
        return channels.stream()
                .mapToInt(c -> c.getOffset() + (c.getType().equals("UINT32") || c.getType().equals("INT32") ? 1 : 0))
                .max()
                .orElse(0);
    }

    private Object processChannel(Channel channel, int[] registers) {

        int offset = channel.getOffset();
        int size = channel.getSize();
        int scale = channel.getScale();

        if (size==1) {
            return process16BitData(registers[offset],scale);
        } else if (size==2) {
            int[] channelRegisters = {registers[offset], registers[offset + 1]};
            return process32BitData(channelRegisters, scale);
        }
        return null;
    }

    private Object process16BitData(int register, int scale) {
        return register/(double)scale;
    }

    private Object process32BitData(int[] registers, int scale) {
        long combined = ((long) registers[0] << 16) | registers[1];

        return combined/(double)scale;
    }

    private void sendMqttMessage(Map<String, Object> data, Map<String, String> tags) throws Exception {
        try (MqttClient client = new MqttClient(this.broker, MqttClient.generateClientId())) {
            client.connect();

            Map<String, Object> payload = new HashMap<>();
            payload.put("data", data);
            payload.put("tags", tags);

            ObjectMapper objectMapper = new ObjectMapper();
            String messagePayload = objectMapper.writeValueAsString(payload);

            MqttMessage message = new MqttMessage(messagePayload.getBytes());
            log.info("Sending message: {}", message);

            client.publish(topic, message);
            client.disconnect();
        }
    }
}
