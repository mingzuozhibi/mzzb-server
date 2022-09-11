package com.mingzuozhibi.modules.core;

import java.util.function.Function;

public class VarBean<T> {

    private final String key;
    private final Function<T, String> format;
    private final VarableRepository repository;
    private T value;

    public VarBean(String key, T value, Function<T, String> format, VarableRepository repository) {
        this.key = key;
        this.value = value;
        this.format = format;
        this.repository = repository;
    }

    public void setValue(T value) {
        this.value = value;
        this.repository.update(key, format.apply(value));
    }

    public T getValue() {
        return value;
    }

}
