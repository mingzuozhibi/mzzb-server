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

import static com.mingzuozhibi.commons.utils.FormatUtils.fmtDateTime;
import static com.mingzuozhibi.commons.utils.MyTimeUtils.toInstant;

@Component
public class SpiderListener extends BaseSupport {

    @Autowired
    private SpiderUpdater spiderUpdater;

    @JmsListener(destination = "prev.update.discs")
    public void listenPrevUpdateDiscs(String json) {
        TypeToken<?> typeToken = TypeToken.getParameterized(ArrayList.class, DiscUpdate.class);
        List<DiscUpdate> discUpdates = gson.fromJson(json, typeToken.getType());
        jmsMessage.notify("JMS <- prev.update.discs size=" + discUpdates.size());
        spiderUpdater.updateDiscs(discUpdates, Instant.now());
    }

    @JmsListener(destination = "last.update.discs")
    public void listenLastUpdateDiscs(String json) {
        JsonObject object = gson.fromJson(json, JsonObject.class);
        LocalDateTime date = gson.fromJson(object.get("date"), LocalDateTime.class);
        TypeToken<?> typeToken = TypeToken.getParameterized(ArrayList.class, DiscUpdate.class);
        List<DiscUpdate> discUpdates = gson.fromJson(object.get("updatedDiscs"), typeToken.getType());
        jmsMessage.notify("JMS <- last.update.discs time=%s, size=%d",
            date.format(fmtDateTime), discUpdates.size());
        spiderUpdater.updateDiscs(discUpdates, toInstant(date));
    }

}
