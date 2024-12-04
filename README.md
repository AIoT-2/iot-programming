## iot-programming

iot programming 실습 진도 확인용 저장소입니다.

---

### 제출 방법
main에서 분기하여 브랜치를 생성하고 자신의 학번으로 네이밍 설정 후 push합니다.

> Ex) ATGN02-019

---

### *2024-12-04*

- `MqttProperty.java` 가 `config.json` 파일을 <br>
절대 경로(`/src/main/java/resources/config.json`)로 읽어오는 방식에서, <br>
**클래스패스 리소스 로딩(Classpath Resource Loading)** 으로 파일을 읽어 오도록 개선

```java
import com.nhnacademy.util.MqttProperty;

// Before
InputStream inputStream = new FileInputStream("/src/main/java/resources/config.json");

// After
InputStream inputStream = MqttProperty.class.getResourceAsStream("/config.json");
```

- Mqtt 로직 구조를 클래스화 시작

---
