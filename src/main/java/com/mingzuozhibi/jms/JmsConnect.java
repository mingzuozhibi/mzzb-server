package com.mingzuozhibi.jms;

import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JmsConnect {

    @Value("${spring.application.name}")
    private String moduleName;

    @Autowired
    private JmsService jmsService;

    public void connect(String moduleAddr) {
        JsonObject root = new JsonObject();
        root.addProperty("name", moduleName);
        root.addProperty("addr", moduleAddr);
        String json = root.toString();
        jmsService.sendJson("module.connect", json, json);
    }

}
