package com.mingzuozhibi.commons.base;

import com.mingzuozhibi.commons.mylog.JmsMessage;
import com.mingzuozhibi.commons.result.ResultSupport;
import org.springframework.beans.factory.annotation.Autowired;

public class BaseService extends ResultSupport {

    @Autowired
    protected JmsMessage jmsMessage;

}
