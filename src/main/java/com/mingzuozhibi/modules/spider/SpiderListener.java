package com.mingzuozhibi.modules.spider;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.commons.mylog.JmsEnums.Name;
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
        TypeToken<?> token = TypeToken.getParameterized(ArrayList.class, DiscUpdate.class);
        List<DiscUpdate> discUpdates = gson.fromJson(json, token.getType());
        jmsSender.bind(Name.SERVER_DISC)
            .debug("JMS <- prev.update.discs size=%d", discUpdates.size());
        spiderUpdater.updateDiscs(discUpdates, Instant.now());
    }

    @JmsListener(destination = "last.update.discs")
    public void listenLastUpdateDiscs(String json) {
        JsonObject object = gson.fromJson(json, JsonObject.class);
        LocalDateTime date = gson.fromJson(object.get("date"), LocalDateTime.class);
        TypeToken<?> token = TypeToken.getParameterized(ArrayList.class, DiscUpdate.class);
        List<DiscUpdate> discUpdates = gson.fromJson(object.get("updatedDiscs"), token.getType());
        jmsSender.bind(Name.SERVER_DISC)
            .debug("JMS <- last.update.discs time=%s, size=%d", date.format(fmtDateTime), discUpdates.size());
        spiderUpdater.updateDiscs(discUpdates, toInstant(date));
    }

}
