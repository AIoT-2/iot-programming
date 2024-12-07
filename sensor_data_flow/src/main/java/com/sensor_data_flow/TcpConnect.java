package com.sensor_data_flow;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;

// TCP 연결을 설정하는 클래스.
public class TcpConnect {
    private static final Logger logger = LoggerFactory.getLogger(TcpConnect.class);
    private static final String DEFAULT_IP_ADDRESS = "192.168.70.203"; // 기본 IP 주소
    private static final int DEFAULT_PORT = Modbus.TCP_PORT; // 기본 포트
    private static final boolean DEFAULT_SET_KEEP_ALIVE = true; // 기본 keepAlive 설정
    private static final TcpParameters tcpParameters = new TcpParameters(); // TCP 파라미터

    /**
     * 지정된 IP 주소, 포트, keepAlive 설정을 사용하여 TCP 연결을 초기화합니다.
     *
     * @param ipAddress    서버의 IP 주소
     * @param port         서버의 포트
     * @param setKeepAlive keepAlive 설정 여부
     */
    public TcpConnect(String ipAddress, int port, boolean setKeepAlive) {
        validateNonEmptyString(ipAddress, "IP 주소는 null이거나 비어 있을 수 없습니다.");
        if (port <= 0) {
            throw new IllegalArgumentException("포트는 0보다 커야 합니다.");
        }

        try {
            // 연결할 서버의 IP 주소 설정
            tcpParameters.setHost(InetAddress.getByName(ipAddress));
        } catch (UnknownHostException e) {
            logger.error("유효하지 않은 IP 주소입니다. {}", e.getMessage());
        }

        // 포트와 keepAlive 설정
        tcpParameters.setPort(port);
        tcpParameters.setKeepAlive(setKeepAlive);
    }

    /**
     * 지정된 IP 주소와 포트를 사용하여 TCP 연결을 초기화합니다.
     * 기본적으로 keepAlive 설정은 true로 설정됩니다.
     *
     * @param ipAddress 서버의 IP 주소
     * @param port      서버의 포트
     */
    public TcpConnect(String ipAddress, int port) {
        this(ipAddress, port, DEFAULT_SET_KEEP_ALIVE);
    }

    /**
     * 기본 IP 주소와 포트를 사용하여 TCP 연결을 초기화합니다.
     */
    public TcpConnect() {
        this(DEFAULT_IP_ADDRESS, DEFAULT_PORT);
    }

    /**
     * 현재 TCP 파라미터를 반환합니다.
     *
     * @return 현재 TCP 파라미터
     */
    public TcpParameters getTcpParameters() {
        return tcpParameters;
    }

    /**
     * 문자열이 null이거나 비어 있는지 확인하는 유틸리티 메서드.
     *
     * @param value        검사할 문자열
     * @param errorMessage 예외 발생 시 사용할 오류 메시지
     */
    private void validateNonEmptyString(String value, String errorMessage) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
