package com.mingzuozhibi.modules.spider;

import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.commons.domain.SearchTask;
import com.mingzuozhibi.commons.mylog.JmsService;
import com.mingzuozhibi.utils.ThreadUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class DiscSpider extends BaseSupport {

    @Autowired
    private JmsService jmsService;

    private final Map<String, SearchTask<DiscInfo>> waitMap = Collections.synchronizedMap(new HashMap<>());

    public SearchTask<DiscInfo> sendDiscUpdate(String asin) {
        SearchTask<DiscInfo> task = new SearchTask<>(asin);
        jmsService.sendJson("send.disc.update", gson.toJson(task), "sendDiscUpdate[" + asin + "]");
        String uuid = task.getUuid();
        waitMap.put(uuid, task);

        ThreadUtils.waitSecond(task, 30);

        return waitMap.remove(uuid);
    }

    @JmsListener(destination = "back.disc.update")
    public void listenDiscUpdate(String json) {
        SearchTask<DiscInfo> task = SearchTask.fromJson(json, DiscInfo.class);
        SearchTask<DiscInfo> lock = waitMap.remove(task.getUuid());
        if (lock != null) {
            waitMap.put(task.getUuid(), task);
            ThreadUtils.notifyAll(lock);
        }
    }

}
