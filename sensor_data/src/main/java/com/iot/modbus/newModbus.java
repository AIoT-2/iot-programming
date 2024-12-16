// package com.iot.modbus;

// import java.io.File;
// import java.time.Instant;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;

// import org.eclipse.paho.client.mqttv3.MqttClient;
// import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
// import org.eclipse.paho.client.mqttv3.MqttException;
// import org.eclipse.paho.client.mqttv3.MqttMessage;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// import com.fasterxml.jackson.core.exc.StreamReadException;
// import com.fasterxml.jackson.core.type.TypeReference;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.serotonin.modbus4j.ModbusFactory;
// import com.serotonin.modbus4j.ModbusMaster;
// import com.serotonin.modbus4j.exception.ModbusTransportException;
// import com.serotonin.modbus4j.msg.ReadHoldingRegistersRequest;
// import com.serotonin.modbus4j.msg.ReadHoldingRegistersResponse;

// public class newModbus extends newDataProcessing implements Runnable {
// static final Logger logger = LoggerFactory.getLogger(ModbusTcpClient.class);

// private static final String broker = "tcp://localhost:1883";
// private static final String clientId = "songs";
// private static final String topic = "songs/data";

// private static final String locationFilePath =
// "src/main/java/com/iot/modbus/location.json";
// private static final String channelFilePath =
// "src/main/java/com/iot/modbus/channel.json";

// // public static void main(String[] args) {
// // Thread modbusThread = new Thread(new ModbusTcpClient());
// // modbusThread.start();
// // }

// @Override
// public void run() {
// try (MqttClient mqttClient = new MqttClient(broker, clientId)) {
// MqttConnectOptions connOpts = new MqttConnectOptions();
// connOpts.setCleanSession(true);
// mqttClient.connect(connOpts);
// logger.info("Connected to MQTT broker: {}", broker);

// List<Map<String, Object>> locationList = loadJsonData(locationFilePath);
// List<Map<String, Object>> channelList = loadJsonData(channelFilePath);

// if (locationList == null || channelList == null) {
// logger.error("Failed to load configuration files.");
// return;
// }

// try (ModbusMaster master = createModbusMaster()) {
// master.init();
// logger.info("Connected to Modbus server.");

// processModbusData(locationList, channelList, master, mqttClient);
// } catch (ModbusTransportException e) {
// logger.error("Error during Modbus transport: {}", e.getMessage());
// }
// } catch (MqttException e) {
// logger.error("MQTT communication error: {}", e.getMessage());
// }
// }

// private ModbusMaster createModbusMaster() {
// ModbusFactory factory = new ModbusFactory();
// return factory.createTcpMaster(new IpParameters() {
// {
// setHost("192.168.70.203");
// setPort(502);
// }
// }, true);
// }
// }
