package com.mingzuozhibi.commons.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Getter
@NoArgsConstructor
public class Result<T> {

    private boolean success;

    private String message;

    private T data;

    private ResultPage page;

    public boolean hasError() {
        return message != null;
    }

    public boolean hasData() {
        return data != null;
    }

    public boolean hasPage() {
        return page != null;
    }

    public boolean isData(Predicate<T> predicate) {
        return hasData() && predicate.test(data);
    }

    public Result<T> ifSuccess(Consumer<T> consumer) {
        if (success) {
            consumer.accept(data);
        }
        return this;
    }

    public Result<T> ifFailure(Consumer<String> consumer) {
        if (success) {
            consumer.accept(message);
        }
        return this;
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

    public static <T> Result<T> ofTask(SearchTask<T> task) {
        if (task.isSuccess()) {
            return ofData(task.getData());
        } else {
            return ofError(task.getMessage());
        }
    }

}
