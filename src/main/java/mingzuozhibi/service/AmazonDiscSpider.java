package mingzuozhibi.service;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.Disc.DiscType;
import mingzuozhibi.persist.disc.Sakura;
import mingzuozhibi.support.Dao;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
public class AmazonDiscSpider {

    private static Logger LOGGER = LoggerFactory.getLogger(AmazonDiscSpider.class);

    @Value("${BCLOUD_IP}")
    private String bcloudIp;

    @Autowired
    private Dao dao;

    public JSONObject fetchDiscInfo(String asin) {
        LOGGER.info("开始更新日亚碟片, ASIN={}", asin);
        return new JSONObject(discInfosAsinGet(asin));
    }

    private LocalDateTime prevTime = null;

    @Transactional
    public void fetchFromBCloud() {
        LOGGER.info("开始更新日亚排名");

        discRanksActivePut(needUpdateAsins(dao.session()));

        JSONObject root = new JSONObject(discRanksActiveGet());

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

                LocalDate date = LocalDate.parse(discInfo.getString("date"), formatter);
                if (date.isAfter(disc.getReleaseDate())) {
                    disc.setReleaseDate(date);
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
            updateSakuraModifyTime();
        } else {
            LOGGER.warn("未能更新日亚排名");
        }

        prevTime = updateOn;
    }

    private void updateSakuraModifyTime() {
        Comparator<Disc> comparator = comparing(Disc::getUpdateTime, nullsFirst(naturalOrder()));
        dao.findBy(Sakura.class, "enabled", true).forEach(sakura -> {
            sakura.getDiscs().stream().max(comparator).ifPresent(disc -> {
                sakura.setModifyTime(disc.getUpdateTime());
            });
        });
    }

    private String discRanksActivePut(Set<String> asins) {
        JSONArray array = new JSONArray();
        asins.forEach(array::put);

        String url = "http://" + bcloudIp + ":8762/discRanks/active";
        Exception lastThrown = null;
        for (int retry = 0; retry < 3; retry++) {
            try {
                return Jsoup.connect(url)
                        .header("Content-Type", "application/json;charset=utf-8")
                        .requestBody(array.toString())
                        .ignoreContentType(true)
                        .method(Method.PUT)
                        .timeout(10000)
                        .execute()
                        .body();
            } catch (Exception e) {
                lastThrown = e;
            }
        }
        String format = "Jsoup: 无法获取网页内容[url=%s][message=%s]";
        String message = String.format(format, url, lastThrown.getMessage());
        throw new RuntimeException(message, lastThrown);
    }

    private String discRanksActiveGet() {
        return getJSON("http://" + bcloudIp + ":8762/discRanks/active");
    }

    private String discInfosAsinGet(String asin) {
        return getJSON("http://" + bcloudIp + ":8762/discInfos/" + asin);
    }

    private String getJSON(String url) {
        Exception lastThrown = null;
        for (int retry = 0; retry < 3; retry++) {
            try {
                return Jsoup.connect(url)
                        .ignoreContentType(true)
                        .method(Method.GET)
                        .timeout(30000)
                        .execute()
                        .body();
            } catch (Exception e) {
                lastThrown = e;
            }
        }
        String format = "Jsoup: 无法获取网页内容[url=%s][message=%s]";
        String message = String.format(format, url, lastThrown.getMessage());
        throw new RuntimeException(message, lastThrown);
    }

}
