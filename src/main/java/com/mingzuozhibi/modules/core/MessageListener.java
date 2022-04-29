package com.mingzuozhibi.modules.core;

import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.commons.mylog.JmsEnums.Name;
import com.mingzuozhibi.commons.mylog.JmsEnums.Type;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
public class MessageListener extends BaseSupport {

    @Autowired
    private MessageRepository messageRepository;

    @JmsListener(destination = "listen.message")
    public void listenMessage(String json) {
        Message message = gson.fromJson(json, Message.class);
        saveMessage(message);
    }

    private void saveMessage(Message message) {
        try {
            if (message.getText().length() > 1000) {
                message.setText(message.getText().substring(0, 1000));
                log.info("saveMessage({})", message);
            }
            messageRepository.save(message.withAccept());
        } catch (Exception e) {
            log.warn("saveMessage({}): {}", message, e);
        }
    }

    @JmsListener(destination = "module.message")
    public void moduleMessage(String json) {
        JsonObject root = gson.fromJson(json, JsonObject.class);
        JsonObject data = root.get("data").getAsJsonObject();
        Message message = new Message();
        setName(root.get("name").getAsString(), message);
        setType(data.get("type").getAsString(), message);
        message.setText(data.get("text").getAsString());
        message.setCreateOn(Instant.ofEpochMilli(data.get("createOn").getAsLong()));
        saveMessage(message);
        log.debug("JMS <- module.message [name={}, data={}]", root.get("name").getAsString(), data);
    }

    private void setType(String type, Message message) {
        switch (type) {
            case "info":
                message.setType(Type.INFO);
                break;
            case "notify":
                message.setType(Type.NOTIFY);
                break;
            case "success":
                message.setType(Type.SUCCESS);
                break;
            case "warning":
                message.setType(Type.WARNING);
                break;
            case "danger":
                message.setType(Type.ERROR);
                break;
            default:
                message.setType(Type.DEBUG);
                break;
        }
    }

    private void setName(String name, Message message) {
        switch (name) {
            case "mzzb-disc-spider":
                message.setName(Name.SPIDER_CONTENT);
                break;
            case "mzzb-disc-shelfs":
                message.setName(Name.SPIDER_HISTORY);
                break;
            default:
                message.setName(Name.DEFAULT);
                break;
        }
    }

}
