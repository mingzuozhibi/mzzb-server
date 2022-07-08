package com.mingzuozhibi.modules.spider;

import com.google.gson.reflect.TypeToken;
import com.mingzuozhibi.commons.amqp.logger.Logger;
import com.mingzuozhibi.commons.amqp.logger.LoggerBind;
import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.modules.admin.VultrService;
import com.mingzuozhibi.modules.disc.DiscRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static com.google.gson.reflect.TypeToken.getParameterized;
import static com.mingzuozhibi.commons.amqp.AmqpEnums.*;
import static com.mingzuozhibi.commons.utils.FormatUtils.fmtDateTime;
import static com.mingzuozhibi.commons.utils.MyTimeUtils.toInstant;
import static java.util.Collections.synchronizedList;

@Component
@LoggerBind(Name.SERVER_DISC)
public class SpiderListener extends BaseSupport {

    @Autowired
    private VultrService vultrService;

    @Autowired
    private ContentUpdater contentUpdater;

    @Autowired
    private DiscRepository discRepository;

    @Autowired
    private HistoryRepository historyRepository;

    private final List<History> toReportList = synchronizedList(new LinkedList<>());

    @RabbitListener(queues = DONE_UPDATE_DISCS)
    public void doneUpdateDiscs(String json) {
        TypeToken<?> token = getParameterized(ArrayList.class, Content.class);
        List<Content> contents = gson.fromJson(json, token.getType());
        bind.debug("JMS <- %s size=%d", DONE_UPDATE_DISCS, contents.size());
        contentUpdater.updateDiscs(contents, Instant.now());
    }

    @RabbitListener(queues = PREV_UPDATE_DISCS)
    public void prevUpdateDiscs(String json) {
        TypeToken<?> token = getParameterized(ArrayList.class, Content.class);
        List<Content> contents = gson.fromJson(json, token.getType());
        bind.debug("JMS <- %s size=%d", PREV_UPDATE_DISCS, contents.size());
        contentUpdater.updateDiscs(contents, Instant.now());
        vultrService.setDoneCount(contents.size());
        vultrService.deleteInstance();
    }

    @RabbitListener(queues = LAST_UPDATE_DISCS)
    public void lastUpdateDiscs(String json) {
        DateResult result = gson.fromJson(json, DateResult.class);
        LocalDateTime date = result.getDate();
        List<Content> contents = result.getResult();
        bind.debug("JMS <- %s time=%s, size=%d", LAST_UPDATE_DISCS, date.format(fmtDateTime), contents.size());
        contentUpdater.updateDiscs(contents, toInstant(date));
    }

    @Transactional
    @RabbitListener(queues = HISTORY_UPDATE)
    public void historyUpdate(String json) {
        TypeToken<?> token = getParameterized(ArrayList.class, History.class);
        List<History> histories = gson.fromJson(json, token.getType());
        histories.forEach(history -> {
            Optional<History> byAsin = historyRepository.findByAsin(history.getAsin());
            if (byAsin.isEmpty()) {
                toReportList.add(history);
                history.setTracked(discRepository.existsByAsin(history.getAsin()));
                historyRepository.save(history);
            } else {
                History toUpdate = byAsin.get();
                toUpdate.setTitle(history.getTitle());
                toUpdate.setType(history.getType());
            }
        });
    }

    @RabbitListener(queues = HISTORY_FINISH)
    public void historyFinish(String json) {
        Logger logger = amqpSender.bind(Name.SPIDER_HISTORY);
        ArrayList<History> histories = new ArrayList<>(toReportList);
        histories.forEach(history -> {
            String format = "[发现新碟片][asin=%s][type=%s][title=%s]";
            logger.success(format, history.getAsin(), history.getType(), history.getTitle());
        });
        logger.notify("发现新碟片%d个", histories.size());
        toReportList.removeAll(histories);
    }

}
