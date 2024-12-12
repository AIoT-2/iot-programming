package com.sensor_data_flow;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

class Person {
    String name;
    int age;

    // 생성자
    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    // 정보 출력 메소드
    public void displayInfo() {
        System.out.println("Name: " + name + ", Age: " + age);
    }
}

public class Test {
    List<Thread> threadList = new LinkedList<>();

    public static void main(String[] args) {
        try {
            // 1. 클래스 이름
            String className = "com.sensor_data_flow.Person";

            // 2. 파라미터 타입을 String과 int로 지정
            String[] par = new String[] { "java.lang.String", "int" }; // 기본형 int 사용
            Class<?>[] parameterTypes = new Class<?>[par.length];
            for (int i = 0; i < par.length; i++) {
                try {
                    if (par[i].equals("int")) {
                        parameterTypes[i] = int.class; // 기본형 int 처리
                    } else {
                        parameterTypes[i] = Class.forName(par[i].trim()); // String은 그대로 처리
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            // 3. 동적으로 클래스 로드
            Class<?> clazz = Class.forName(className);

            // 4. 클래스의 생성자 찾기
            Constructor<?> constructor = clazz.getConstructor(parameterTypes);

            // 5. 생성자 호출하여 객체 생성
            Object personInstance = constructor.newInstance("John Doe", 30);

            // 6. 메소드 호출
            Method displayMethod = clazz.getMethod("displayInfo");
            displayMethod.invoke(personInstance);
        } catch (Exception e) {
            e.printStackTrace(); // 예외 출력
        }
    }
}
