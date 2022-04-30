package com.mingzuozhibi.modules.core;

import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.commons.mylog.JmsEnums.Name;
import com.mingzuozhibi.commons.mylog.JmsLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static com.mingzuozhibi.commons.mylog.JmsEnums.MODULE_CONNECT;

@Component
public class ConnectListener extends BaseSupport {

    private JmsLogger bind;

    @PostConstruct
    public void bind() {
        bind = jmsSender.bind(Name.SERVER_CORE);
    }

    @Autowired
    private ConnectService connectService;

    @JmsListener(destination = MODULE_CONNECT)
    public void moduleConnect(String json) {
        JsonObject root = gson.fromJson(json, JsonObject.class);
        String name = root.get("name").getAsString();
        String addr = root.get("addr").getAsString();
        connectService.setModuleAddr(name, addr);
        bind.debug("JMS <- %s name=%s, addr=%s", MODULE_CONNECT, name, addr);
    }

}
