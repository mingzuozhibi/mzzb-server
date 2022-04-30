package com.mingzuozhibi.modules.spider;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.commons.mylog.JmsLogger;
import com.mingzuozhibi.modules.disc.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static com.mingzuozhibi.commons.mylog.JmsEnums.*;
import static com.mingzuozhibi.commons.utils.FormatUtils.fmtDateTime;
import static com.mingzuozhibi.commons.utils.MyTimeUtils.toInstant;
import static javax.xml.transform.OutputKeys.MEDIA_TYPE;

@Component
public class SpiderListener extends BaseSupport {

    private JmsLogger bind;

    @PostConstruct
    public void bind() {
        bind = jmsSender.bind(Name.SERVER_DISC);
    }

    @Autowired
    private GroupService groupService;

    @Autowired
    private DiscContentUpdater discContentUpdater;

    @Transactional
    @Scheduled(cron = "0 59 * * * ?")
    @GetMapping(value = "/admin/sendNeedUpdateAsins", produces = MEDIA_TYPE)
    public void sendNeedUpdateAsins() {
        Set<String> asins = groupService.findNeedUpdateAsinsSorted();
        jmsSender.send(NEED_UPDATE_ASINS, gson.toJson(asins));
        JmsLogger bind = jmsSender.bind(Name.SERVER_DISC);
        bind.debug("JMS -> %s size=%d", NEED_UPDATE_ASINS, asins.size());
    }

    @JmsListener(destination = PREV_UPDATE_DISCS)
    public void prevUpdateDiscs(String json) {
        TypeToken<?> token = TypeToken.getParameterized(ArrayList.class, DiscContent.class);
        List<DiscContent> discContents = gson.fromJson(json, token.getType());
        bind.debug("JMS <- %s size=%d", PREV_UPDATE_DISCS, discContents.size());
        discContentUpdater.updateDiscs(discContents, Instant.now());
    }

    @JmsListener(destination = LAST_UPDATE_DISCS)
    public void lastUpdateDiscs(String json) {
        JsonObject object = gson.fromJson(json, JsonObject.class);
        LocalDateTime date = gson.fromJson(object.get("date"), LocalDateTime.class);
        TypeToken<?> token = TypeToken.getParameterized(ArrayList.class, DiscContent.class);
        List<DiscContent> discContents = gson.fromJson(object.get("updatedDiscs"), token.getType());
        bind.debug("JMS <- %s time=%s, size=%d", LAST_UPDATE_DISCS, date.format(fmtDateTime), discContents.size());
        discContentUpdater.updateDiscs(discContents, toInstant(date));
    }

}
