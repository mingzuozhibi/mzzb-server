package com.mingzuozhibi.commons.base;

import com.google.gson.Gson;
import com.mingzuozhibi.commons.domain.Result;
import com.mingzuozhibi.commons.mylog.JmsMessage;
import org.springframework.beans.factory.annotation.Autowired;

import static com.mingzuozhibi.commons.gson.GsonFactory.GSON;

public abstract class BaseSupport {

    @Autowired
    protected Gson gson;

    @Autowired
    protected JmsMessage jmsMessage;

    public static String errorResult(String error) {
        return GSON.toJson(Result.ofError(error));
    }

}
