package com.mingzuozhibi.commons.domain;

import lombok.*;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Result<T> {

    public Result(boolean success, String message, T data, ResultPage page) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.page = page;
    }

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

    public static <T> Result<T> ofError(String error) {
        return new Result<>(false, error, null, null);
    }

    public static <T> Result<T> ofData(T data) {
        return new Result<>(true, null, data, null);
    }

    public static <T> Result<List<T>> ofPage(Page<T> page) {
        return new Result<>(true, null, page.getContent(), new ResultPage(page));
    }

}
