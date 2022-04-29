package com.mingzuozhibi.modules.core;

import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.base.BaseSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageListener extends BaseSupport {

    @Autowired
    private MessageService messageService;

    @JmsListener(destination = "module.message")
    public void moduleMessage(String json) {
        JsonObject root = gson.fromJson(json, JsonObject.class);
        String name = root.get("name").getAsString();
        JsonObject data = root.get("data").getAsJsonObject();
        messageService.saveMessage(name, data);
        log.debug("JMS <- module.message [name={}, data={}]", name, data);
    }

}
