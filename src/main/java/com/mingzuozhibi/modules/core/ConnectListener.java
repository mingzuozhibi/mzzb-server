package com.mingzuozhibi.modules.core;

import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.base.BaseSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ConnectListener extends BaseSupport {

    @Autowired
    private ConnectService connectService;

    @JmsListener(destination = "module.connect")
    public void moduleConnect(String json) {
        JsonObject root = gson.fromJson(json, JsonObject.class);
        String name = root.get("name").getAsString();
        String addr = root.get("addr").getAsString();
        connectService.setModuleAddr(name, addr);
        log.info("JMS <- module.connect [name={}, addr={}]", name, addr);
    }

}
