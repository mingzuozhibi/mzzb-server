package com.mingzuozhibi.modules.spider;

import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.commons.domain.Result;
import com.mingzuozhibi.commons.domain.SearchTask;
import com.mingzuozhibi.commons.mylog.JmsService;
import com.mingzuozhibi.modules.disc.Disc;
import com.mingzuozhibi.modules.disc.Disc.DiscType;
import com.mingzuozhibi.utils.ThreadUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

import static com.mingzuozhibi.commons.utils.FormatUtils.fmtDate;

@Component
public class DiscUpdateApi extends BaseSupport {

    @Autowired
    private JmsService jmsService;

    private final Map<String, SearchTask<DiscUpdate>> waitMap = Collections.synchronizedMap(new HashMap<>());

    public Result<DiscUpdate> doGet(String asin) {
        SearchTask<DiscUpdate> task = sendDiscUpdate(asin);
        if (!task.isSuccess()) {
            return Result.ofError(task.getMessage());
        }
        DiscUpdate discUpdate = task.getData();
        if (discUpdate.isOffTheShelf()) {
            return Result.ofError("可能该碟片已下架");
        }
        if (!Objects.equals(asin, discUpdate.getAsin())) {
            return Result.ofError(String.format("ASIN不符合, [%s]=>[%s]", asin, discUpdate.getAsin()));
        }
        return Result.ofData(task.getData());
    }

    public Disc createWith(DiscUpdate discUpdate) {
        String asin = discUpdate.getAsin();
        String title = discUpdate.getTitle();
        DiscType discType = DiscType.valueOf(discUpdate.getType());
        LocalDate releaseDate = Optional.ofNullable(discUpdate.getDate())
            .map(date -> LocalDate.parse(date, fmtDate))
            .orElse(null);
        return new Disc(asin, title, discType, releaseDate);
    }

    private SearchTask<DiscUpdate> sendDiscUpdate(String asin) {
        SearchTask<DiscUpdate> task = new SearchTask<>(asin);
        jmsService.sendJson("send.disc.update", gson.toJson(task), "sendDiscUpdate[" + asin + "]");
        String uuid = task.getUuid();
        waitMap.put(uuid, task);

        ThreadUtils.waitSecond(task, 30);

        return waitMap.remove(uuid);
    }

    @JmsListener(destination = "back.disc.update")
    public void listenDiscUpdate(String json) {
        SearchTask<DiscUpdate> task = SearchTask.fromJson(json, DiscUpdate.class);
        SearchTask<DiscUpdate> lock = waitMap.remove(task.getUuid());
        if (lock != null) {
            waitMap.put(task.getUuid(), task);
            ThreadUtils.notifyAll(lock);
        }
    }

}