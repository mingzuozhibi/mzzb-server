package com.mingzuozhibi.modules.spider;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.commons.mylog.JmsLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.mingzuozhibi.commons.mylog.JmsEnums.*;
import static com.mingzuozhibi.commons.utils.FormatUtils.fmtDateTime;
import static com.mingzuozhibi.commons.utils.MyTimeUtils.toInstant;

@Component
public class SpiderListener extends BaseSupport {

    private JmsLogger bind;

    @PostConstruct
    public void bind() {
        bind = jmsSender.bind(Name.SERVER_DISC);
    }

    @Autowired
    private SpiderUpdater spiderUpdater;

    @JmsListener(destination = PREV_UPDATE_DISCS)
    public void prevUpdateDiscs(String json) {
        TypeToken<?> token = TypeToken.getParameterized(ArrayList.class, DiscUpdate.class);
        List<DiscUpdate> discUpdates = gson.fromJson(json, token.getType());
        bind.debug("JMS <- %s size=%d", PREV_UPDATE_DISCS, discUpdates.size());
        spiderUpdater.updateDiscs(discUpdates, Instant.now());
    }

    @JmsListener(destination = LAST_UPDATE_DISCS)
    public void lastUpdateDiscs(String json) {
        JsonObject object = gson.fromJson(json, JsonObject.class);
        LocalDateTime date = gson.fromJson(object.get("date"), LocalDateTime.class);
        TypeToken<?> token = TypeToken.getParameterized(ArrayList.class, DiscUpdate.class);
        List<DiscUpdate> discUpdates = gson.fromJson(object.get("updatedDiscs"), token.getType());
        bind.debug("JMS <- %s time=%s, size=%d", LAST_UPDATE_DISCS, date.format(fmtDateTime), discUpdates.size());
        spiderUpdater.updateDiscs(discUpdates, toInstant(date));
    }

}
