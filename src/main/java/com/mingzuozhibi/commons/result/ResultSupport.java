package com.mingzuozhibi.commons.result;

import org.springframework.data.domain.Page;

import static com.mingzuozhibi.commons.gson.GsonFactory.GSON;

public abstract class ResultSupport {

    public static String errorResult(String error) {
        return GSON.toJson(Result.ofError(error));
    }

    protected <T> String dataResult(T data) {
        return GSON.toJson(Result.ofData(data));
    }

    protected <T> String pageResult(Page<T> page) {
        return GSON.toJson(Result.ofPage(page));
    }

}
