package com.mingzuozhibi.commons.base;

import com.mingzuozhibi.commons.mylog.JmsMessage;
import com.mingzuozhibi.commons.result.ResultSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

public class BaseController2 extends ResultSupport {

    protected static final String MEDIA_TYPE = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    protected JmsMessage jmsMessage;

}
