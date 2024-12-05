## iot-programming

iot programming 실습 진도 확인용 저장소입니다.

---

### 제출 방법
main에서 분기하여 브랜치를 생성하고 자신의 학번으로 네이밍 설정 후 push합니다.

> Ex) ATGN02-019

---

### *2024-12-05*

- `Jackson` 라이브러리의 다양한 테스트 케이스 예제를 작성한 덕분에, <br>
  `Property.java`의 구조를 이전보다 유연하게 구현 완료하였습니다.

- `logback` 로그 파일을 동적 경로를 통해서 저장되도록 설계 및 구현 완료

```xml
<!-- pom.xml 내부 -->

<project>
    <properties>
        <log.dir>${project.basedir}/logs</log.dir>
    </properties>

    <build>
        <plugins>
            <!-- Maven Resources Plugin 설정 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-resources-plugin</artifactId>
                <version>3.3.0</version>
                <executions>
                    <execution>
                        <phase>process-resources</phase>
                        <goals>
                            <goal>copy-resources</goal>
                        </goals>
                        <configuration>
                            <resources>
                                <resource>
                                    <directory>src/main/resources</directory>  <!-- 리소스 경로 -->
                                    <includes>
                                        <include>logback.xml</include>  <!-- 필터링할 리소스 지정 -->
                                    </includes>
                                    <filtering>true</filtering>  <!-- 필터링 활성화 -->
                                </resource>
                            </resources>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```


```xml
<!-- logback.xml 내부 -->

<configuration>
    <!-- log.dir 변수 설정 -->
    <property name="log.dir" value="logs"/>

    <!-- =========================================================================================================== -->

    <!-- 파일에 로그를 기록하는 Appender 설정 -->
    <appender name="ROLLING_LOG_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <!-- 로그 파일 경로 및 이름 지정 -->
        <!--<file>${user.home}/logs/app-log.log</file>-->
        <file>${log.dir}/app-log.log</file>

        <!-- 로그 롤링 정책 설정 -->
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${log.dir}/app-log-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>10MB</maxFileSize>
            <maxHistory>30</maxHistory>
        </rollingPolicy>

        <!-- 로그 포맷 설정 -->
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- =========================================================================================================== -->

    <!-- ConsoleAppender (콘솔에 로그 출력) -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- =========================================================================================================== -->

    <!-- 로그 레벨과 로그 출력을 지정하는 로거 설정 -->

    <root level="DEBUG">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="ROLLING_LOG_FILE"/>
    </root>

</configuration>
```

---

### *2024-12-04*

- `MqttProperty.java` 가 `config.json` 파일을 <br>
  절대 경로(`/src/main/java/resources/config.json`)로 읽어오는 방식에서, <br>
  **클래스패스 리소스 로딩(Classpath Resource Loading)** 으로 파일을 읽어 오도록 개선

```java
import com.nhnacademy.util.Property;

// Before
InputStream inputStream = new FileInputStream("/src/main/java/resources/config.json");

        // After
        InputStream inputStream = Property.class.getResourceAsStream("/config.json");
```

- Mqtt 로직 구조를 클래스화 시작

---
