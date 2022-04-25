package com.mingzuozhibi.commons.mylog;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JmsService {

    @Value("${spring.application.name}")
    private String moduleName;

    @Autowired
    private JmsTemplate template;

    public void sendJson(String destination, String json, String message) {
        convertAndSend(destination, json);
        log.info("JMS -> {}: {}", destination, message);
    }

    public void sendJson(String destination, String json) {
        convertAndSend(destination, json);
        log.info("JMS -> {}: {}", destination, json);
    }

    public void convertAndSend(String destination, String json) {
        for (int i = 0; i < 3; i++) {
            try {
                template.convertAndSend(destination, json);
                break;
            } catch (JmsException e) {
                String format = "convertAndSend(destination=%s, json=%s)";
                log.debug(String.format(format, destination, json), e);
            }
        }
    }

    public String buildJson(JsonElement data) {
        JsonObject root = new JsonObject();
        root.addProperty("name", moduleName);
        root.add("data", data);
        return root.toString();
    }

}
