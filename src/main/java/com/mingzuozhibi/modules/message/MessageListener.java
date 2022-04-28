package com.mingzuozhibi.modules.message;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import static com.mingzuozhibi.commons.gson.GsonFactory.GSON;

@Slf4j
@Component
public class MessageListener {

    @Autowired
    private MessageService messageService;

    @JmsListener(destination = "module.message")
    public void moduleMessage(String json) {
        JsonObject root = GSON.fromJson(json, JsonObject.class);
        String name = root.get("name").getAsString();
        JsonObject data = root.get("data").getAsJsonObject();
        messageService.saveMessage(name, data);
        log.debug("JMS <- module.message [name={}, data={}]", name, data);
    }

}
