package com.mingzuozhibi.commons.base;

import com.google.gson.Gson;
import com.mingzuozhibi.commons.mylog.JmsMessage;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseService {

    @Autowired
    protected Gson gson;

    @Autowired
    protected JmsMessage jmsMessage;

}
