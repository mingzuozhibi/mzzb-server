package com.mingzuozhibi.jms;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
public class JmsMessageImpl implements JmsMessage {

    @Autowired
    private JmsService jmsService;

    @Override
    public void info(String format, Object... args) {
        info(String.format(format, args));
    }

    @Override
    public void success(String format, Object... args) {
        success(String.format(format, args));
    }

    @Override
    public void notify(String format, Object... args) {
        notify(String.format(format, args));
    }

    @Override
    public void warning(String format, Object... args) {
        warning(String.format(format, args));
    }

    @Override
    public void danger(String format, Object... args) {
        danger(String.format(format, args));
    }

    @Override
    public void info(String message) {
        sendMsgNoLog("info", message);
        log.info("JMS -> {}: {}", "module.message", message);
    }

    @Override
    public void success(String message) {
        sendMsgNoLog("success", message);
        log.info("JMS -> {}: {}", "module.message", message);
    }

    @Override
    public void notify(String message) {
        sendMsgNoLog("notify", message);
        log.info("JMS -> {}: {}", "module.message", message);
    }

    @Override
    public void warning(String message) {
        sendMsgNoLog("warning", message);
        log.warn("JMS -> {}: {}", "module.message", message);
    }

    @Override
    public void danger(String message) {
        sendMsgNoLog("danger", message);
        log.error("JMS -> {}: {}", "module.message", message);
    }

    public String buildMsg(String type, String text) {
        JsonObject data = new JsonObject();
        data.addProperty("type", type);
        data.addProperty("text", text);
        data.addProperty("createOn", Instant.now().toEpochMilli());
        return jmsService.buildJson(data);
    }

    public void sendMsgNoLog(String type, String message) {
        jmsService.sendJson("module.message", buildMsg(type, message));
    }

    public void infoAndSend(String type, String message) {
        jmsService.sendJson("module.message", buildMsg(type, message), message);
    }

    public void infoAndSend(String type, String message, String infoLog) {
        jmsService.sendJson("module.message", buildMsg(type, message), infoLog);
    }

}
