package mingzuozhibi.service;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.Disc.DiscType;
import mingzuozhibi.persist.disc.DiscGroup;
import mingzuozhibi.support.Dao;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;

import static java.util.Comparator.*;
import static mingzuozhibi.utils.DiscUtils.needUpdateAsins;

@Service
public class DiscInfosSpider {

    private static Logger LOGGER = LoggerFactory.getLogger(DiscInfosSpider.class);

    @Autowired
    private Dao dao;

    @Autowired
    private SpiderHelper spiderHelper;

    @Transactional
    public JSONObject searchDisc(String asin) {
        LOGGER.info("开始更新日亚碟片, ASIN={}", asin);
        return new JSONObject(fetchDiscInfoByAsin(asin));
    }

    private LocalDateTime prevTime = null;

    @Transactional
    public void fetchFromBCloud() {
        LOGGER.info("开始更新日亚排名");

        pushNeedUpdateAsins(needUpdateAsins(dao.session()));
        JSONObject root = new JSONObject(pullPrevUpdateDiscs());

        LocalDateTime updateOn = Instant.ofEpochMilli(root.getLong("updateOn"))
                .atZone(ZoneId.systemDefault()).toLocalDateTime();

        if (prevTime != null && prevTime.isEqual(updateOn)) {
            LOGGER.info("日亚排名没有变化");
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");

        JSONArray discInfos = root.getJSONArray("data");
        for (int i = 0; i < discInfos.length(); i++) {
            JSONObject discInfo = discInfos.getJSONObject(i);
            String asin = discInfo.getString("asin");
            Disc disc = dao.lookup(Disc.class, "asin", asin);
            if (disc != null) {
                disc.setTitle(discInfo.getString("title"));

                if (disc.getDiscType() == DiscType.Auto || disc.getDiscType() == DiscType.Other) {
                    disc.setDiscType(DiscType.valueOf(discInfo.getString("type")));
                }

                String dateString = discInfo.getString("date");
                if (StringUtils.isNotEmpty(dateString)) {
                    LocalDate date = LocalDate.parse(dateString, formatter);
                    if (date.compareTo(disc.getReleaseDate()) != 0) {
                        LOGGER.info("Update Disc Release Date: {} => {}",
                                disc.getReleaseDate().format(formatter), date.format(formatter));
                        disc.setReleaseDate(date);
                    }
                }

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
        }

        if (discInfos.length() > 0) {
            LOGGER.info("成功更新日亚排名：共{}个", discInfos.length());
            updateDiscGroupModifyTime();
        } else {
            LOGGER.warn("未能更新日亚排名");
        }

        prevTime = updateOn;
    }

    private void updateDiscGroupModifyTime() {
        Comparator<Disc> comparator = comparing(Disc::getUpdateTime, nullsFirst(naturalOrder()));
        dao.findBy(DiscGroup.class, "enabled", true).forEach(discGroup -> {
            discGroup.getDiscs().stream().max(comparator).ifPresent(disc -> {
                discGroup.setModifyTime(disc.getUpdateTime());
            });
        });
    }

    private String pullPrevUpdateDiscs() {
        String url = spiderHelper.mzzbSpider("/discRanks/active");
        return spiderHelper.waitRequest(url);
    }

    private String pushNeedUpdateAsins(Set<String> asins) {
        JSONArray array = new JSONArray();
        asins.forEach(array::put);
        String body = array.toString();

        String url = spiderHelper.mzzbSpider("/discRanks/active");
        return spiderHelper.waitRequest(url, connection -> {
            connection.header("Content-Type", "application/json;charset=utf-8");
            connection.method(Method.PUT);
            connection.requestBody(body);
        });
    }

    private String fetchDiscInfoByAsin(String asin) {
        String url = spiderHelper.mzzbSpider("/discInfos/%s", asin);
        return spiderHelper.waitRequest(url);
    }

}
