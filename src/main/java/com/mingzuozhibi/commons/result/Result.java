package com.mingzuozhibi.commons.result;


import com.google.gson.reflect.TypeToken;
import lombok.Value;
import org.springframework.data.domain.Page;

import java.lang.reflect.Type;
import java.util.List;

import static com.mingzuozhibi.commons.gson.GsonFactory.GSON;

@Value
public class Result<T> {

    boolean success;

    String error;

    T data;

    ResultPage page;

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

    public static <T> Result<T> ofJson(String json, Type... typeArguments) {
        return GSON.fromJson(json, getParameterizedType(typeArguments));
    }

    public static Type getParameterizedType(Type... typeArguments) {
        return TypeToken.getParameterized(Result.class, typeArguments).getType();
    }

}
