package com.mingzuozhibi.modules.core;

import java.util.function.Function;

public class VarBean<T> {

    private final String key;
    private final Function<T, String> format;
    private final VarableService service;
    private T value;

    public VarBean(String key, T value, Function<T, String> format, VarableService service) {
        this.key = key;
        this.value = value;
        this.format = format;
        this.service = service;
    }

    public void setValue(T value) {
        this.value = value;
        this.service.update(key, format.apply(value));
    }

    public T getValue() {
        return value;
    }

}
