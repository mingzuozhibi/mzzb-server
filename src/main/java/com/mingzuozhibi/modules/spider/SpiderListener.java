package com.mingzuozhibi.modules.spider;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.commons.mylog.JmsBind;
import com.mingzuozhibi.commons.mylog.JmsLogger;
import com.mingzuozhibi.modules.disc.DiscRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static com.google.gson.reflect.TypeToken.getParameterized;
import static com.mingzuozhibi.commons.mylog.JmsEnums.*;
import static com.mingzuozhibi.commons.utils.FormatUtils.fmtDateTime;
import static com.mingzuozhibi.commons.utils.MyTimeUtils.toInstant;

@Component
@JmsBind(Name.SERVER_DISC)
public class SpiderListener extends BaseSupport {

    @Autowired
    private ContentUpdater contentUpdater;

    @Autowired
    private DiscRepository discRepository;

    @Autowired
    private HistoryRepository historyRepository;

    private final List<History> historyList =
        Collections.synchronizedList(new LinkedList<>());

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

    @Transactional
    @JmsListener(destination = HISTORY_UPDATE)
    public void historyUpdate(String json) {
        TypeToken<?> token = getParameterized(ArrayList.class, History.class);
        List<History> historyList = gson.fromJson(json, token.getType());
        historyList.forEach(history -> {
            Optional<History> byAsin = historyRepository.findByAsin(history.getAsin());
            if (!byAsin.isPresent()) {
                historyList.add(history);
                history.setTracked(discRepository.existsByAsin(history.getAsin()));
                historyRepository.save(history);
            } else {
                History toUpdate = byAsin.get();
                toUpdate.setTitle(history.getTitle());
                toUpdate.setType(history.getType());
            }
        });
    }

    @JmsListener(destination = HISTORY_FINISH)
    public void historyFinish(String json) {
        JmsLogger logger = jmsSender.bind(Name.SPIDER_HISTORY);
        ArrayList<History> list = new ArrayList<>(historyList);
        list.forEach(history -> {
            String format = "[发现新碟片][asin=%s][type=%s][title=%s]";
            logger.success(format, history.getAsin(), history.getType(), history.getTitle());
        });
        logger.notify("发现新碟片%d个", list.size());
        historyList.removeAll(list);
    }

}
