package com.mingzuozhibi.utils;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class JmsHelper {

    @Autowired
    private JmsTemplate jmsTemplate;

    public void sendDiscTrack(String asin, String title) {
        JSONObject root = new JSONObject();
        root.put("name", title);
        root.put("asin", asin);
        jmsTemplate.convertAndSend("disc.track", root.toString());
    }

}
