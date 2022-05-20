package com.mingzuozhibi.commons.amqp.logger;

import com.mingzuozhibi.commons.amqp.AmqpEnums.Name;
import com.mingzuozhibi.commons.amqp.AmqpSender;
import lombok.extern.slf4j.Slf4j;

import static com.mingzuozhibi.commons.amqp.AmqpEnums.Type.*;

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

    public void debug(String format, Object... args) {
        amqpSender.info(name, DEBUG, String.format(format, args));
    }

    public void info(String format, Object... args) {
        amqpSender.info(name, INFO, String.format(format, args));
    }

    public void notify(String format, Object... args) {
        amqpSender.info(name, NOTIFY, String.format(format, args));
    }

    public void success(String format, Object... args) {
        amqpSender.info(name, SUCCESS, String.format(format, args));
    }

    public void warning(String format, Object... args) {
        amqpSender.info(name, WARNING, String.format(format, args));
    }

    public void error(String format, Object... args) {
        amqpSender.info(name, ERROR, String.format(format, args));
    }

}
