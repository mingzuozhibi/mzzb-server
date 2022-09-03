package com.mingzuozhibi.commons.logger;

import com.mingzuozhibi.commons.amqp.AmqpSender;
import com.mingzuozhibi.commons.base.BaseKeys.Name;
import lombok.extern.slf4j.Slf4j;

import static com.mingzuozhibi.commons.base.BaseKeys.Type.*;

@Slf4j
public class Logger {

    private final Name name;
    private final AmqpSender amqpSender;

    public Logger(Name name, AmqpSender amqpSender) {
        this.amqpSender = amqpSender;
        this.name = name;
    }

    public void debug(String text) {
        amqpSender.info(name, DEBUG, text);
    }

    public void info(String text) {
        amqpSender.info(name, INFO, text);
    }

    public void notify(String text) {
        amqpSender.info(name, NOTIFY, text);
    }

    public void success(String text) {
        amqpSender.info(name, SUCCESS, text);
    }

    public void warning(String text) {
        amqpSender.info(name, WARNING, text);
    }

    public void error(String text) {
        amqpSender.info(name, ERROR, text);
    }

}
