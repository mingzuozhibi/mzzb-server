package com.mingzuozhibi.modules.spider;

import com.google.gson.reflect.TypeToken;
import com.mingzuozhibi.commons.amqp.logger.LoggerBind;
import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.commons.domain.Result;
import com.mingzuozhibi.commons.domain.SearchTask;
import com.mingzuozhibi.commons.utils.ThreadUtils;
import com.mingzuozhibi.modules.disc.Disc;
import com.mingzuozhibi.modules.disc.Disc.DiscType;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static com.mingzuozhibi.commons.amqp.AmqpEnums.*;
import static com.mingzuozhibi.commons.utils.FormatUtils.fmtDate;
import static com.mingzuozhibi.support.ModifyUtils.getName;

@Component
@LoggerBind(Name.SERVER_USER)
public class ContentService extends BaseSupport {

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
        amqpSender.send(CONTENT_SEARCH, gson.toJson(task));
        String uuid = task.getUuid();
        waitMap.put(uuid, task);

        long time = Instant.now().toEpochMilli();
        ThreadUtils.waitSecond(task, 30);
        long cost = Instant.now().toEpochMilli() - time;

        SearchTask<Content> remove = waitMap.remove(uuid);
        if (remove == task) {
            bind.warning("[%s][查询碟片超时][asin=%s][cost=%d ms]", getName(), asin, cost);
        } else if (!remove.isSuccess()) {
            String format = "[%s][查询碟片失败][asin=%s][cost=%d ms][error=%s]";
            bind.warning(format, getName(), asin, cost, remove.getMessage());
        } else {
            bind.success("[%s][查询碟片成功][asin=%s][cost=%d ms]", getName(), asin, cost);
        }
        return remove;
    }

    @RabbitListener(queues = CONTENT_RETURN)
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
