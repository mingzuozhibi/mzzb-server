package com.mingzuozhibi.modules.vultr;

import com.google.gson.reflect.TypeToken;
import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.commons.logger.LoggerBind;
import com.mingzuozhibi.modules.spider.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

import static com.google.gson.reflect.TypeToken.getParameterized;
import static com.mingzuozhibi.commons.base.BaseKeys.*;

@Component
@LoggerBind(Name.SERVER_CORE)
public class VultrListener extends BaseSupport {

    @Autowired
    private VultrService vultrService;

    @Autowired
    private HistoryUpdater historyUpdater;

    @Autowired
    private ContentUpdater contentUpdater;

    @RabbitListener(queues = HISTORY_FINISH)
    public void historyFinish(String json) {
        var token = TypeToken.getParameterized(List.class, History.class);
        List<History> histories = gson.fromJson(json, token.getType());
        bind.debug("JMS <- %s size=%d".formatted(HISTORY_FINISH, histories.size()));
        historyUpdater.updateAllHistory(histories);
    }

    @RabbitListener(queues = CONTENT_FINISH)
    public void contentFinish(String json) {
        TypeToken<?> token = getParameterized(List.class, Content.class);
        List<Content> contents = gson.fromJson(json, token.getType());
        bind.debug("JMS <- %s size=%d".formatted(CONTENT_FINISH, contents.size()));
        contentUpdater.updateAllContent(contents, Instant.now());
    }

    @RabbitListener(queues = FETCH_TASK_DONE1)
    public void fetchTaskDone1(String json) {
        if (Boolean.parseBoolean(json)) {
            bind.warning("更新上架信息失败");
            vultrService.createServer();
        } else {
            bind.warning("更新上架信息成功");
            vultrService.setStartted(true);
        }
    }

    @RabbitListener(queues = FETCH_TASK_DONE2)
    public void fetchTaskDone2(String json) {
        int doneCount = Integer.parseInt(json);
        vultrService.finishServer(doneCount);
    }

}
