package com.mingzuozhibi.modules.spider;

import com.google.gson.reflect.TypeToken;
import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.commons.domain.Result;
import com.mingzuozhibi.commons.domain.SearchTask;
import com.mingzuozhibi.commons.utils.ThreadUtils;
import com.mingzuozhibi.modules.disc.Disc;
import com.mingzuozhibi.modules.disc.Disc.DiscType;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

import static com.mingzuozhibi.commons.mylog.JmsEnums.*;
import static com.mingzuozhibi.commons.utils.FormatUtils.fmtDate;

@Component
public class DiscContentApi extends BaseSupport {

    private final Map<String, SearchTask<DiscContent>> waitMap = Collections.synchronizedMap(new HashMap<>());

    public Result<DiscContent> doGet(String asin) {
        SearchTask<DiscContent> task = contentSearch(asin);
        return Result.ofTask(task).ifSuccess((data, result) -> {
            if (data.isOffTheShelf()) {
                result.withError("可能该碟片已下架");
            }
        });
    }

    public Disc createWith(DiscContent discContent) {
        String asin = discContent.getAsin();
        String title = discContent.getTitle();
        DiscType discType = DiscType.valueOf(discContent.getType());
        LocalDate releaseDate = Optional.ofNullable(discContent.getDate())
            .map(date -> LocalDate.parse(date, fmtDate))
            .orElse(null);
        return new Disc(asin, title, discType, releaseDate);
    }

    private SearchTask<DiscContent> contentSearch(String asin) {
        SearchTask<DiscContent> task = new SearchTask<>(asin);
        jmsSender.send(CONTENT_SEARCH, gson.toJson(task));
        String uuid = task.getUuid();
        waitMap.put(uuid, task);

        ThreadUtils.waitSecond(task, 30);

        return waitMap.remove(uuid);
    }

    @JmsListener(destination = CONTENT_RETURN)
    public void contentReturn(String json) {
        TypeToken<?> token = TypeToken.getParameterized(SearchTask.class, DiscContent.class);
        SearchTask<DiscContent> task = gson.fromJson(json, token.getType());
        SearchTask<DiscContent> lock = waitMap.remove(task.getUuid());
        if (lock != null) {
            waitMap.put(task.getUuid(), task);
            ThreadUtils.notifyAll(lock);
        }
    }

}
