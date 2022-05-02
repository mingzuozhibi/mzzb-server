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
public class ContentApi extends BaseSupport {

    private final Map<String, SearchTask<Content>> waitMap = Collections.synchronizedMap(new HashMap<>());

    public Result<Content> doGet(String asin) {
        SearchTask<Content> task = contentSearch(asin);
        return Result.ofTask(task).ifSuccess((data, result) -> {
            if (data.isOffTheShelf()) {
                result.withError("可能该碟片已下架");
            }
        });
    }

    public Disc createWith(Content content) {
        String asin = content.getAsin();
        String title = content.getTitle();
        DiscType discType = DiscType.valueOf(content.getType());
        LocalDate releaseDate = Optional.ofNullable(content.getDate())
            .map(date -> LocalDate.parse(date, fmtDate))
            .orElse(null);
        return new Disc(asin, title, discType, releaseDate);
    }

    private SearchTask<Content> contentSearch(String asin) {
        SearchTask<Content> task = new SearchTask<>(asin);
        jmsSender.send(CONTENT_SEARCH, gson.toJson(task));
        String uuid = task.getUuid();
        waitMap.put(uuid, task);

        ThreadUtils.waitSecond(task, 30);

        return waitMap.remove(uuid);
    }

    @JmsListener(destination = CONTENT_RETURN)
    public void contentReturn(String json) {
        TypeToken<?> token = TypeToken.getParameterized(SearchTask.class, Content.class);
        SearchTask<Content> task = gson.fromJson(json, token.getType());
        SearchTask<Content> lock = waitMap.remove(task.getUuid());
        if (lock != null) {
            waitMap.put(task.getUuid(), task);
            ThreadUtils.notifyAll(lock);
        }
    }

}