package com.mingzuozhibi.commons.mylog;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
public class JmsMessage {

    @Autowired
    private JmsService jmsService;

    public void info(String format, Object... args) {
        info(String.format(format, args));
    }

    public void success(String format, Object... args) {
        success(String.format(format, args));
    }

    public void notify(String format, Object... args) {
        notify(String.format(format, args));
    }

    public void warning(String format, Object... args) {
        warning(String.format(format, args));
    }

    public void danger(String format, Object... args) {
        danger(String.format(format, args));
    }

    public void info(String message) {
        jmsService.convertAndSend("module.message", buildMsg("info", message));
        log.info("JMS -> {}: {}", "module.message", message);
    }

    public void success(String message) {
        jmsService.convertAndSend("module.message", buildMsg("success", message));
        log.info("JMS -> {}: {}", "module.message", message);
    }

    public void notify(String message) {
        jmsService.convertAndSend("module.message", buildMsg("notify", message));
        log.info("JMS -> {}: {}", "module.message", message);
    }

    public void warning(String message) {
        jmsService.convertAndSend("module.message", buildMsg("warning", message));
        log.warn("JMS -> {}: {}", "module.message", message);
    }

    public void danger(String message) {
        jmsService.convertAndSend("module.message", buildMsg("danger", message));
        log.error("JMS -> {}: {}", "module.message", message);
    }

    private String buildMsg(String type, String text) {
        JsonObject data = new JsonObject();
        data.addProperty("type", type);
        data.addProperty("text", text);
        data.addProperty("createOn", Instant.now().toEpochMilli());
        return jmsService.buildJson(data);
    }

}
