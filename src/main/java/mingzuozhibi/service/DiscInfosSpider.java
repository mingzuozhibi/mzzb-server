package mingzuozhibi.service;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.Disc.DiscType;
import mingzuozhibi.persist.disc.DiscGroup;
import mingzuozhibi.support.Dao;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Objects;

import static java.util.Comparator.*;

@Service
public class DiscInfosSpider {

    private static Logger LOGGER = LoggerFactory.getLogger(DiscInfosSpider.class);

    @Autowired
    private Dao dao;

    @Autowired
    private SpiderHelper spiderHelper;

    @Transactional
    public JSONObject fetchDisc(String asin) {
        LOGGER.info("开始更新日亚碟片, ASIN={}", asin);
        String url = spiderHelper.gateway("/fetchDisc/%s", asin);
        return new JSONObject(spiderHelper.waitRequest(url, connection -> {
            connection.timeout(60 * 1000);
        }));
    }

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Transactional
    public void updateDiscInfos(JSONArray discInfos) {
        LOGGER.info("开始更新日亚排名");
        LocalDateTime updateOn = LocalDateTime.now();

        for (int i = 0; i < discInfos.length(); i++) {
            JSONObject discInfo = discInfos.getJSONObject(i);
            try {
                updateDiscInfo(updateOn, formatter, discInfo);
            } catch (RuntimeException e) {
                LOGGER.warn("更新碟片时遇到一个错误，数据：" + discInfo.toString(), e);
            }
        }

        if (discInfos.length() > 0) {
            LOGGER.info("成功更新日亚排名：共{}个", discInfos.length());
            updateDiscGroupModifyTime();
        } else {
            LOGGER.warn("未能更新日亚排名");
        }
    }

    private void updateDiscInfo(LocalDateTime updateOn, DateTimeFormatter formatter, JSONObject discInfo) {
        String asin = discInfo.getString("asin");
        Disc disc = dao.lookup(Disc.class, "asin", asin);

        if (disc != null) {

            updateTitle(discInfo, disc);

            updateType(discInfo, disc);

            updateDate(formatter, discInfo, disc);

            updateRank(updateOn, discInfo, disc);
        }
    }

    private void updateTitle(JSONObject discInfo, Disc disc) {
        disc.setTitle(discInfo.getString("title"));
    }

    private void updateType(JSONObject discInfo, Disc disc) {
        if (disc.getDiscType() == DiscType.Auto || disc.getDiscType() == DiscType.Other) {
            disc.setDiscType(DiscType.valueOf(discInfo.getString("type")));
        }
    }

    private void updateDate(DateTimeFormatter formatter, JSONObject discInfo, Disc disc) {
        if (discInfo.has("date")) {
            String dateString = discInfo.getString("date");
            LocalDate date = LocalDate.parse(dateString, formatter);
            if (date.isAfter(disc.getReleaseDate())) {
                LOGGER.info("Update Disc Release Date: {} => {}",
                    disc.getReleaseDate().format(formatter), date.format(formatter));
                disc.setReleaseDate(date);
            }
        }
    }

    private void updateRank(LocalDateTime updateOn, JSONObject discInfo, Disc disc) {
        if (discInfo.has("rank")) {
            if (disc.getModifyTime() == null || updateOn.isAfter(disc.getModifyTime())) {
                disc.setPrevRank(disc.getThisRank());
                disc.setThisRank(discInfo.getInt("rank"));
                if (!Objects.equals(disc.getThisRank(), disc.getPrevRank())) {
                    disc.setModifyTime(updateOn);
                }
                disc.setUpdateTime(updateOn);
            }
        }
    }

    private void updateDiscGroupModifyTime() {
        Comparator<Disc> comparator = comparing(Disc::getUpdateTime, nullsFirst(naturalOrder()));
        dao.findBy(DiscGroup.class, "enabled", true).forEach(discGroup -> {
            discGroup.getDiscs().stream().max(comparator).ifPresent(disc -> {
                discGroup.setModifyTime(disc.getUpdateTime());
            });
        });
    }

}
