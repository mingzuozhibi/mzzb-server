package com.mingzuozhibi.modules.spider;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mingzuozhibi.commons.mylog.JmsMessage;
import com.mingzuozhibi.commons.mylog.JmsService;
import com.mingzuozhibi.modules.disc.Disc;
import com.mingzuozhibi.modules.disc.Disc.DiscType;
import com.mingzuozhibi.modules.disc.DiscRepository;
import com.mingzuozhibi.modules.group.DiscGroupRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
public class DiscSpider {

    @Autowired
    private Gson gson;

    @Autowired
    private JmsService jmsService;

    @Autowired
    private JmsMessage jmsMessage;

    @Autowired
    private DiscRepository discRepository;

    @Autowired
    private DiscGroupRepository discGroupRepository;

    private final Map<String, SearchTask<DiscUpdate>> waitMap = Collections.synchronizedMap(new HashMap<>());

    public SearchTask<DiscUpdate> runSearchTask(String asin) {
        SearchTask<DiscUpdate> task = new SearchTask<>(asin);
        jmsService.sendJson("send.disc.update", gson.toJson(task), "runSearchTask[" + asin + "]");
        String uuid = task.getUuid();
        waitMap.put(uuid, task);

        WaitUtils.waitSecond(task, 30);

        return waitMap.remove(uuid);
    }

    @JmsListener(destination = "back.disc.update")
    public void listenDiscUpdate(String json) {
        TypeToken<?> typeToken = TypeToken.getParameterized(SearchTask.class, DiscUpdate.class);
        SearchTask<DiscUpdate> task = gson.fromJson(json, typeToken.getType());
        String uuid = task.getUuid();

        SearchTask<DiscUpdate> remove = waitMap.remove(uuid);
        if (remove != null) {
            waitMap.put(uuid, task);
            WaitUtils.notifyAll(remove);
        }
    }

    @Transactional
    public void applyDiscUpdates(List<DiscUpdate> discUpdates) {
        try {
            jmsMessage.notify("开始更新日亚排名");
            LocalDateTime updateOn = LocalDateTime.now();
            for (DiscUpdate discUpdate : discUpdates) {
                for (int i = 0; i < 3; i++) {
                    try {
                        applyDiscUpdate(discUpdate, updateOn);
                        break;
                    } catch (Exception e) {
                        jmsMessage.warning("未能更新日亚排名(%d/3)：%s", i + 1, e.getMessage());
                    }
                }
            }

            if (discUpdates.size() > 0) {
                discGroupRepository.updateModifyTime();
                jmsMessage.notify("成功更新日亚排名：共%d个", discUpdates.size());
            } else {
                jmsMessage.notify("未能更新日亚排名：无数据");
            }
        } catch (Exception e) {
            jmsMessage.warning("未能更新日亚排名：%s", e.getMessage());
        }
    }

    private void applyDiscUpdate(DiscUpdate discUpdate, LocalDateTime updateOn) {
        String asin = discUpdate.getAsin();
        Optional<Disc> byAsin = discRepository.findByAsin(asin);
        if (!byAsin.isPresent()) {
            jmsMessage.warning("[应用碟片更新时，发现未知碟片][%s]", asin);
            return;
        }
        Disc disc = byAsin.get();
        if (discUpdate.isOffTheShelf()) {
            jmsMessage.warning("[碟片可能已下架][%s]", asin);
            return;
        }
        updateTitle(disc, discUpdate);
        updateType(disc, discUpdate);
        updateDate(disc, discUpdate);
        updateRank(disc, discUpdate, updateOn);
    }

    private void updateTitle(Disc disc, DiscUpdate discUpdate) {
        String title = discUpdate.getTitle();
        if (!Objects.equals(title, disc.getTitle())) {
            jmsMessage.info("[碟片标题更新][%s => %s][%s]", disc.getTitle(), title, disc.getAsin());
            disc.setTitle(title);
        }
    }

    private void updateType(Disc disc, DiscUpdate discUpdate) {
        DiscType type = DiscType.valueOf(discUpdate.getType());
        if (disc.getDiscType() == DiscType.Auto || disc.getDiscType() == DiscType.Other) {
            disc.setDiscType(type);
        }
        if (!Objects.equals(type, disc.getDiscType())) {
            jmsMessage.warning("[碟片类型不符][%s => %s][%s]", disc.getDiscType(), type, disc.getAsin());
        }
    }

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private void updateDate(Disc disc, DiscUpdate discUpdate) {
        if (!StringUtils.hasLength(discUpdate.getDate())) {
            jmsMessage.info("[发售时间为空][当前设置为%s][%s]", disc.getReleaseDate(), disc.getAsin());
            return;
        }
        LocalDate date = LocalDate.parse(discUpdate.getDate(), formatter);
        boolean buyset = discUpdate.isBuyset();
        if (date.isAfter(disc.getReleaseDate()) && !buyset) {
            jmsMessage.info("[发售时间更新][%s => %s][%s]", disc.getReleaseDate(), date, disc.getAsin());
            disc.setReleaseDate(date);
        }
        if (!Objects.equals(date, disc.getReleaseDate())) {
            if (buyset) {
                jmsMessage.info("[发售时间不符][%s => %s][%s][套装=true]", disc.getReleaseDate(), date, disc.getAsin());
            } else {
                jmsMessage.warning("[发售时间不符][%s => %s][%s][套装=false]", disc.getReleaseDate(), date, disc.getAsin());
            }
        }
    }

    private void updateRank(Disc disc, DiscUpdate discUpdate, LocalDateTime updateOn) {
        if (disc.getModifyTime() == null || updateOn.isAfter(disc.getModifyTime())) {
            disc.setPrevRank(disc.getThisRank());
            disc.setThisRank(discUpdate.getRank());
            if (!Objects.equals(disc.getThisRank(), disc.getPrevRank())) {
                disc.setModifyTime(updateOn);
            }
            disc.setUpdateTime(updateOn);
        }
    }

}
