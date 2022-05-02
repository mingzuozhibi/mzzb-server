package com.mingzuozhibi.modules.spider;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.commons.mylog.JmsLogger;
import com.mingzuozhibi.modules.disc.DiscRepository;
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

import static com.google.gson.reflect.TypeToken.getParameterized;
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
    private ContentUpdater contentUpdater;

    @Autowired
    private DiscRepository discRepository;

    @Autowired
    private HistoryRepository historyRepository;

    @Transactional
    @Scheduled(cron = "0 59 * * * ?")
    @GetMapping(value = "/admin/sendNeedUpdateAsins", produces = MEDIA_TYPE)
    public void sendNeedUpdateAsins() {
        Set<String> asins = groupService.findNeedUpdateAsinsSorted();
        jmsSender.send(NEED_UPDATE_ASINS, gson.toJson(asins));
        JmsLogger bind = jmsSender.bind(Name.SERVER_DISC);
        bind.debug("JMS -> %s size=%d", NEED_UPDATE_ASINS, asins.size());
    }

    @JmsListener(destination = DONE_UPDATE_DISCS)
    public void doneUpdateDiscs(String json) {
        TypeToken<?> token = getParameterized(ArrayList.class, Content.class);
        List<Content> contents = gson.fromJson(json, token.getType());
        bind.debug("JMS <- %s size=%d", DONE_UPDATE_DISCS, contents.size());
        contentUpdater.updateDiscs(contents, Instant.now());
    }

    @JmsListener(destination = PREV_UPDATE_DISCS)
    public void prevUpdateDiscs(String json) {
        TypeToken<?> token = getParameterized(ArrayList.class, Content.class);
        List<Content> contents = gson.fromJson(json, token.getType());
        bind.debug("JMS <- %s size=%d", PREV_UPDATE_DISCS, contents.size());
        contentUpdater.updateDiscs(contents, Instant.now());
    }

    @JmsListener(destination = LAST_UPDATE_DISCS)
    public void lastUpdateDiscs(String json) {
        JsonObject object = gson.fromJson(json, JsonObject.class);
        LocalDateTime date = gson.fromJson(object.get("date"), LocalDateTime.class);
        TypeToken<?> token = getParameterized(ArrayList.class, Content.class);
        List<Content> contents = gson.fromJson(object.get("updatedDiscs"), token.getType());
        bind.debug("JMS <- %s time=%s, size=%d", LAST_UPDATE_DISCS, date.format(fmtDateTime), contents.size());
        contentUpdater.updateDiscs(contents, toInstant(date));
    }

    @JmsListener(destination = HISTORY_UPDATE)
    public void historyUpdate(String json) {
        TypeToken<?> token = getParameterized(ArrayList.class, History.class);
        List<History> historyList = gson.fromJson(json, token.getType());
        historyList.forEach(history -> {
            history.setTracked(discRepository.existsByAsin(history.getAsin()));
            if (!historyRepository.existsByAsin(history.getAsin())) {
                historyRepository.save(history);
                String format = "[发现新碟片][asin=%s][type=%s][title=%s]";
                jmsSender.bind(Name.SPIDER_HISTORY)
                    .success(format, history.getAsin(), history.getType(), history.getTitle());
            }
        });
    }

}
