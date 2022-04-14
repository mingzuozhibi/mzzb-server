package com.mingzuozhibi.gateway.modules.discshelfs;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.base.BaseController;
import com.mingzuozhibi.commons.mylog.JmsMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RestController
public class DiscShelfsListener extends BaseController {

    @Autowired
    private JmsMessage jmsMessage;

    @Resource(name = "redisTemplate")
    private SetOperations<String, String> setOps;

    @JmsListener(destination = "disc.track")
    public void discTrack(String json) {
        JsonObject root = new Gson().fromJson(json, JsonObject.class);
        String name = root.get("name").getAsString();
        String asin = root.get("asin").getAsString();
        setOps.add("disc.track", asin);
        jmsMessage.notify("JMS <- disc.track [name=%s, asin=%s]", name, asin);
    }

}
