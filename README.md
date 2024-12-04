# iot-programming
iot programming 실습 진도 확인용 저장소입니다.

# 코드 최적화 및 가용성을 위한 변경 필요

### Modbus2.java -> 통합데이터를 받아서 전처리 후 다른 Broker에 전달

### RecvMqtt.java -> 디바이스 정보 담긴 broker에 접속하여 필요한 데이터를 분리 후 다른 broker에 데이터를 전달

### SendMqtt.java -> 보낼 broker에 접속하여 전송할 수 있게 해줌 

### TotalMqtt.java -> 정리된 데이터를 받은 broker쪽에서 데이터를 받아 influxDB에 삽입