package com.mingzuozhibi.modules.spider;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.mingzuozhibi.commons.base.BaseSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.mingzuozhibi.commons.gson.GsonFactory.GSON;
import static com.mingzuozhibi.commons.utils.FormatUtils.fmtDateTime;
import static com.mingzuozhibi.commons.utils.MyTimeUtils.toInstant;

@Component
public class SpiderListener extends BaseSupport {

    @Autowired
    private SpiderUpdater spiderUpdater;

    @JmsListener(destination = "prev.update.discs")
    public void listenPrevUpdateDiscs(String json) {
        TypeToken<?> token = TypeToken.getParameterized(ArrayList.class, DiscUpdate.class);
        List<DiscUpdate> discUpdates = GSON.fromJson(json, token.getType());
        jmsMessage.notify("JMS <- prev.update.discs size=" + discUpdates.size());
        spiderUpdater.updateDiscs(discUpdates, Instant.now());
    }

    @JmsListener(destination = "last.update.discs")
    public void listenLastUpdateDiscs(String json) {
        JsonObject object = GSON.fromJson(json, JsonObject.class);
        LocalDateTime date = GSON.fromJson(object.get("date"), LocalDateTime.class);
        TypeToken<?> token = TypeToken.getParameterized(ArrayList.class, DiscUpdate.class);
        List<DiscUpdate> discUpdates = GSON.fromJson(object.get("updatedDiscs"), token.getType());
        jmsMessage.notify("JMS <- last.update.discs time=%s, size=%d",
            date.format(fmtDateTime), discUpdates.size());
        spiderUpdater.updateDiscs(discUpdates, toInstant(date));
    }

}
