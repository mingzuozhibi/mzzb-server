package com.mingzuozhibi.commons.result;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Result<T> {

    public static <T> Result<T> ofError(String error) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setError(error);
        return result;
    }

    public static <T> Result<T> ofData(T data) {
        Result<T> result = new Result<>();
        result.setSuccess(true);
        result.setData(data);
        return result;
    }

    private boolean success;
    private String error;
    private T data;

    public boolean hasError() {
        return error != null;
    }

    public boolean hasData() {
        return data != null;
    }

}
