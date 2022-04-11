package com.mingzuozhibi.commons.result;

import com.google.gson.Gson;
import com.mingzuozhibi.commons.mylog.JmsMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import static com.mingzuozhibi.commons.gson.GsonFactory.GSON;

public abstract class ResultSupport {

    @Autowired
    protected Gson gson;

    @Autowired
    protected JmsMessage jmsMessage;

    public static String errorResult(String message) {
        return GSON.toJson(new BaseResult(message));
    }

    protected <T> String dataResult(T data) {
        return gson.toJson(new DataResult<T>(data));
    }

    protected <T> String pageResult(Page<T> page) {
        return gson.toJson(new PageResult<T>(page));
    }

    protected <T> String baseResult(BaseResult baseResult) {
        return gson.toJson(baseResult);
    }

}
