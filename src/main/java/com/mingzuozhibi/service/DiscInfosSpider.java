package com.mingzuozhibi.service;

import com.mingzuozhibi.commons.gson.InstantUtils;
import com.mingzuozhibi.commons.mylog.JmsMessage;
import com.mingzuozhibi.modules.disc.Disc;
import com.mingzuozhibi.modules.disc.Disc.DiscType;
import com.mingzuozhibi.modules.disc.DiscGroup;
import com.mingzuozhibi.support.Dao;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static java.util.Comparator.*;

@Slf4j
@Component
public class DiscInfosSpider {

    @Autowired
    private Dao dao;

    @Autowired
    private JmsMessage jmsMessage;

    @Autowired
    private SpiderHelper spiderHelper;

    @Transactional
    public JSONObject fetchDisc(String asin) {
        log.info("开始更新日亚碟片, ASIN={}", asin);
        String url = spiderHelper.gateway("/fetchDisc/%s", asin);
        return new JSONObject(spiderHelper.waitRequest(url, connection -> {
            connection.timeout(180 * 1000);
        }));
    }

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Transactional
    public void updateDiscInfos(List<DiscInfo> discInfos) {
        jmsMessage.notify("开始更新日亚排名");
        LocalDateTime updateOn = LocalDateTime.now();
        for (DiscInfo info : discInfos) {
            for (int i = 0; i < 3; i++) {
                try {
                    updateDiscInfo(info, updateOn);
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (discInfos.size() > 0) {
            jmsMessage.notify("成功更新日亚排名：共%d个", discInfos.size());
            updateDiscGroupModifyTime();
        } else {
            jmsMessage.warning("未能更新日亚排名");
        }
    }

    private void updateDiscInfo(DiscInfo discInfo, LocalDateTime updateOn) {
        String asin = discInfo.getAsin();
        Disc disc = dao.lookup(Disc.class, "asin", asin);

        if (disc != null) {

            if (!discInfo.isOffTheShelf()) {

                updateTitle(disc, discInfo);

                updateType(disc, discInfo);

                updateDate(disc, discInfo);

            } else {

                jmsMessage.warning("[碟片可能已下架][%s]", asin);

            }

            updateRank(disc, discInfo, updateOn);
        }
    }

    private void updateTitle(Disc disc, DiscInfo discInfo) {
        String title = discInfo.getTitle();
        if (!Objects.equals(title, disc.getTitle())) {
            jmsMessage.info("[碟片标题更新][%s => %s][%s]", disc.getTitle(), title, disc.getAsin());
            disc.setTitle(title);
        }
    }

    private void updateType(Disc disc, DiscInfo discInfo) {
        DiscType type = DiscType.valueOf(discInfo.getType());
        if (disc.getDiscType() == DiscType.Auto || disc.getDiscType() == DiscType.Other) {
            disc.setDiscType(type);
        }
        if (!Objects.equals(type, disc.getDiscType())) {
            jmsMessage.warning("[碟片类型不符][%s => %s][%s]", disc.getDiscType(), type, disc.getAsin());
        }
    }

    private void updateDate(Disc disc, DiscInfo discInfo) {
        if (StringUtils.hasLength(discInfo.getDate())) {
            LocalDate date = LocalDate.parse(discInfo.getDate(), formatter);
            if (date.isAfter(disc.getReleaseDate()) && !discInfo.isBuyset()) {
                jmsMessage.warning("[发售时间更新][%s => %s][%s]", disc.getReleaseDate(), date, disc.getAsin());
                disc.setReleaseDate(date);
            }
            if (!Objects.equals(date, disc.getReleaseDate())) {
                jmsMessage.warning("[发售时间不符][%s => %s][%s][套装=%s]", disc.getReleaseDate(), date, disc.getAsin(),
                    discInfo.isBuyset());
            }
        } else {
            jmsMessage.warning("[发售时间为空][当前设置为%s][%s]", disc.getReleaseDate(), disc.getAsin());
        }
    }

    private void updateRank(Disc disc, DiscInfo discInfo, LocalDateTime updateOn) {
        if (disc.getModifyTime() == null || updateOn.isAfter(disc.getModifyTime())) {
            disc.setPrevRank(disc.getThisRank());
            disc.setThisRank(discInfo.getRank());
            if (!Objects.equals(disc.getThisRank(), disc.getPrevRank())) {
                disc.setModifyTime(updateOn);
            }
            disc.setUpdateTime(updateOn);
        }
    }

    private void updateDiscGroupModifyTime() {
        Comparator<Disc> comparator = comparing(Disc::getUpdateTime, nullsFirst(naturalOrder()));
        dao.findBy(DiscGroup.class, "enabled", true).forEach(discGroup -> {
            discGroup.getDiscs().stream().max(comparator).ifPresent(disc -> {
                discGroup.setModifyTime(InstantUtils.toInstant(disc.getUpdateTime()));
            });
        });
    }

}
