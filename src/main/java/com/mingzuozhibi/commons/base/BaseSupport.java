package com.mingzuozhibi.commons.base;

import com.google.gson.Gson;
import com.mingzuozhibi.commons.amqp.AmqpSender;
import com.mingzuozhibi.commons.amqp.logger.Logger;
import com.mingzuozhibi.commons.domain.Result;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import static com.mingzuozhibi.commons.gson.GsonFactory.GSON;

public abstract class BaseSupport {

    @Autowired
    protected Gson gson;

    @Setter
    protected Logger bind;

    @Autowired
    protected AmqpSender amqpSender;

    public static String errorResult(String error) {
        return GSON.toJson(Result.ofError(error));
    }

}
