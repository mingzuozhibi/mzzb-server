package com.mingzuozhibi.jms;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JmsService {

    @Value("${spring.application.name}")
    private String moduleName;

    @Autowired
    private JmsTemplate template;

    public void sendJson(String destination, String json, String info) {
        template.convertAndSend(destination, json);
        log.info("JMS -> {}: {}", destination, info);
    }

    public void sendJson(String destination, String json) {
        template.convertAndSend(destination, json);
    }

    public String buildJson(JsonElement data) {
        JsonObject root = new JsonObject();
        root.addProperty("name", moduleName);
        root.add("data", data);
        return root.toString();
    }

}
