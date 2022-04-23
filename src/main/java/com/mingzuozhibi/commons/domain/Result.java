package com.mingzuozhibi.commons.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Result<T> {

    public Result(boolean success, String error, T data, ResultPage page) {
        this.success = success;
        this.error = error;
        this.data = data;
        this.page = page;
    }

    private boolean success;

    private String error;

    private T data;

    private ResultPage page;

    public boolean hasError() {
        return error != null;
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
