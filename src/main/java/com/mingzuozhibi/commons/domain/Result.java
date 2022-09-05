package com.mingzuozhibi.commons.domain;

import lombok.Getter;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

@Getter
public class Result<T> {

    private boolean success;
    private String message;
    private T data;
    private ResultPage page;

    public boolean hasError() {
        return !isSuccess();
    }

    public boolean testData(Predicate<T> predicate) {
        return isSuccess() && predicate.test(getData());
    }

    public <R> Result<R> thenData(Function<T, R> function) {
        if (isSuccess()) {
            return ofData(function.apply(getData()));
        } else {
            return ofError(getMessage());
        }
    }

    public static <T> Result<T> ofTask(SearchTask<T> task) {
        if (task.isSuccess()) {
            return ofData(task.getData());
        } else {
            return ofError(task.getMessage());
        }
    }

    public static <T> Result<T> ofError(String message) {
        var result = new Result<T>();
        result.success = false;
        result.message = message;
        return result;
    }

    public static <T> Result<T> ofData(T data) {
        var result = new Result<T>();
        result.success = true;
        result.data = data;
        return result;
    }

    public static <T> Result<List<T>> ofPage(List<T> data, ResultPage page) {
        var result = new Result<List<T>>();
        result.success = true;
        result.data = data;
        result.page = page;
        return result;
    }

}
