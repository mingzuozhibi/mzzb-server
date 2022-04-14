package com.mingzuozhibi.commons.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Getter
@Setter
public class Result<T> {

    private List<Throwable> errors = new LinkedList<>();
    private String errorMessage;
    private T content;

    public void pushError(Throwable e) {
        errors.add(e);
    }

    public String formatError() {
        return Optional.ofNullable(errorMessage).orElseGet(() -> formatErrors(errors));
    }

    public boolean isUnfinished() {
        return content == null;
    }

    public void syncResult(Result<T> result) {
        if (result.isUnfinished()) {
            this.setErrorMessage(result.formatError());
        } else {
            this.setContent(result.getContent());
        }
    }

    public static <T> Result<T> ofContent(T content) {
        Result<T> result = new Result<>();
        result.setContent(content);
        return result;
    }

    public static <T> Result<T> ofErrorCause(Throwable throwable) {
        Result<T> result = new Result<>();
        while (throwable != null) {
            result.pushError(throwable);
            throwable = throwable.getCause();
        }
        return result;
    }

    public static <T> Result<T> ofExceptions(Throwable... throwables) {
        Result<T> result = new Result<>();
        for (Throwable e : throwables) {
            result.pushError(e);
        }
        return result;
    }

    public static <T> Result<T> ofErrorMessage(String errorMessage) {
        Result<T> result = new Result<>();
        result.setErrorMessage(errorMessage);
        return result;
    }

    public static String formatErrorCause(Throwable throwable) {
        return ofErrorCause(throwable).formatError();
    }

    public static String formatErrors(Throwable... throwables) {
        return formatErrors(Arrays.asList(throwables));
    }

    public static String formatErrors(List<Throwable> errors) {
        if (errors == null || errors.isEmpty()) {
            return "[No Error]";
        }
        AtomicInteger count = new AtomicInteger(0);
        return errors.stream()
            .map(e -> e.getClass().getSimpleName() + ": " + e.getMessage())
            .distinct()
            .map(str -> String.format("(%d)[%s]", count.incrementAndGet(), str))
            .collect(Collectors.joining(" "));
    }

}
