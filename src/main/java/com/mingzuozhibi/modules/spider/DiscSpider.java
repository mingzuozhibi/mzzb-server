package com.mingzuozhibi.modules.spider;

import com.google.gson.reflect.TypeToken;
import com.mingzuozhibi.commons.mylog.JmsService;
import com.mingzuozhibi.commons.result.ResultSupport;
import com.mingzuozhibi.commons.utils.ThreadUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class DiscSpider extends ResultSupport {

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
        TypeToken<?> typeToken = TypeToken.getParameterized(SearchTask.class, DiscInfo.class);
        SearchTask<DiscInfo> task = gson.fromJson(json, typeToken.getType());
        String uuid = task.getUuid();

        SearchTask<DiscInfo> remove = waitMap.remove(uuid);
        if (remove != null) {
            waitMap.put(uuid, task);
            ThreadUtils.notifyAll(remove);
        }
    }

}
