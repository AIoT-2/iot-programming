package com.sensor_data_parsing;

/**
 * 센서 데이터의 값을 매핑하는 클래스.
 * 이름, 스케일, 비트 정보를 관리합니다.
 */
public class ValueMapper {
    private String name; // 값의 이름
    private double scale; // 값의 스케일
    private boolean is32bit; // 32비트 여부

    /**
     * 모든 매개변수를 사용하여 ValueMapper 객체를 초기화합니다.
     *
     * @param name    값의 이름
     * @param scale   값의 스케일
     * @param is32bit 32비트 여부
     */
    public ValueMapper(String name, double scale, boolean is32bit) {
        validateString(name, "이름은 null이거나 비어 있을 수 없습니다.");
        this.name = name;
        this.scale = scale;
        this.is32bit = is32bit;
    }

    /**
     * 이름과 스케일을 사용하여 ValueMapper 객체를 초기화합니다.
     * 기본적으로 32비트 여부는 false로 설정됩니다.
     *
     * @param name  값의 이름
     * @param scale 값의 스케일
     */
    public ValueMapper(String name, double scale) {
        this(name, scale, false);
    }

    /**
     * 이름과 32비트 여부를 사용하여 ValueMapper 객체를 초기화합니다.
     * 기본적으로 스케일은 1로 설정됩니다.
     *
     * @param name    값의 이름
     * @param is32bit 32비트 여부
     */
    public ValueMapper(String name, boolean is32bit) {
        this(name, 1, is32bit);
    }

    /**
     * 이름만을 사용하여 ValueMapper 객체를 초기화합니다.
     * 기본적으로 스케일은 1, 32비트 여부는 false로 설정됩니다.
     *
     * @param name 값의 이름
     */
    public ValueMapper(String name) {
        this(name, 1);
    }

    /**
     * 값의 이름을 반환합니다.
     *
     * @return 값의 이름
     */
    public String getName() {
        return name;
    }

    /**
     * 값의 스케일을 반환합니다.
     *
     * @return 값의 스케일
     */
    public double getScale() {
        return scale;
    }

    /**
     * 값이 32비트인지 여부를 반환합니다.
     *
     * @return 32비트 여부
     */
    public boolean getIs32bit() {
        return is32bit;
    }

    /**
     * 값의 이름을 설정합니다.
     *
     * @param name 새 값의 이름
     */
    public void setName(String name) {
        validateString(name, "이름은 null이거나 비어 있을 수 없습니다.");
        this.name = name;
    }

    /**
     * 값의 스케일을 설정합니다.
     *
     * @param scale 새 값의 스케일
     */
    public void setScale(double scale) {
        this.scale = scale;
    }

    /**
     * 값이 32비트인지 여부를 설정합니다.
     *
     * @param is32bit 새 32비트 여부
     */
    public void setIs32bit(boolean is32bit) {
        this.is32bit = is32bit;
    }

    /**
     * 문자열이 null이거나 비어 있는지 확인하는 유틸리티 메서드.
     *
     * @param value        검사할 문자열
     * @param errorMessage 예외 발생 시 사용할 오류 메시지
     */
    private void validateString(String value, String errorMessage) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
