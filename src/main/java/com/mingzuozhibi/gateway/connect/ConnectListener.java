package com.mingzuozhibi.gateway.connect;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.mylog.JmsMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ConnectListener {

    @Autowired
    private JmsMessage jmsMessage;

    @Autowired
    private ConnectService connectService;

    @JmsListener(destination = "module.connect")
    public void moduleConnect(String json) {
        JsonObject root = new Gson().fromJson(json, JsonObject.class);
        String name = root.get("name").getAsString();
        String addr = root.get("addr").getAsString();
        connectService.setModuleAddr(name, addr);
        jmsMessage.info("JMS <- module.connect [name=%s, addr=%s]", name, addr);
    }

}
