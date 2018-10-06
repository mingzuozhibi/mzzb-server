package mingzuozhibi.service;

import mingzuozhibi.persist.disc.Disc;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class AmazonDiscSpider {

    private static Logger LOGGER = LoggerFactory.getLogger(AmazonDiscSpider.class);

    @Value("${BCLOUD_IP}")
    private String bcloudIp;

    @Autowired
    private Dao dao;

    public JSONObject fetchDiscInfo(String asin) {
        LOGGER.info("开始更新日亚碟片, ASIN={}", asin);
        JSONObject root = new JSONObject(discInfosAsinGet(asin));
        if (root.getBoolean("success")) {
            return root.getJSONObject("data");
        }
        return null;
    }

    @Transactional
    public void fetchFromBCloud() {
        LOGGER.info("开始更新日亚排名");
        Set<String> asins = new HashSet<>();
        dao.findBy(Sakura.class, "enabled", true)
                .forEach(sakura -> {
                    sakura.getDiscs().forEach(disc -> {
                        asins.add(disc.getAsin());
                    });
                });
        discRanksActivePut(asins);
        JSONObject root = new JSONObject(discRanksActiveGet());
        Map<String, Integer> results = new HashMap<>();
        JSONArray discRanks = root.getJSONArray("data");
        for (int i = 0; i < discRanks.length(); i++) {
            JSONObject discRank = discRanks.getJSONObject(i);
            String asin = discRank.getString("asin");
            if (discRank.has("rank")) {
                results.put(asin, discRank.getInt("rank"));
            }
        }
        LocalDateTime modifyTime = Instant.ofEpochMilli(root.getLong("updateOn"))
                .atZone(ZoneId.systemDefault()).toLocalDateTime();
        finishTheUpdate(results, modifyTime);
    }

    private void finishTheUpdate(Map<String, Integer> results, LocalDateTime updateOn) {
        LOGGER.info("[正在写入Amazon(ALL)数据]]");
        results.forEach((asin, rank) -> {
            Disc disc = dao.lookup(Disc.class, "asin", asin);
            if (disc != null) {
                if (disc.getModifyTime() == null || updateOn.isAfter(disc.getModifyTime())) {
                    disc.setPrevRank(disc.getThisRank());
                    disc.setThisRank(rank);
                    if (!Objects.equals(disc.getThisRank(), disc.getPrevRank())) {
                        disc.setModifyTime(updateOn);
                    }
                    disc.setUpdateTime(updateOn);
                }
            }
        });
        dao.findAll(Sakura.class).stream()
                .filter(Sakura::isEnabled)
                .forEach(sakura -> {
                    sakura.setModifyTime(updateOn);
                });
        LOGGER.info("[成功更新Amazon(ALL)数据]");
    }

    private String discRanksActivePut(Set<String> asins) {
        JSONArray array = new JSONArray();
        asins.forEach(array::put);

        System.out.println(array.toString().length());

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
        String url = "http://" + bcloudIp + ":8762/discRanks/active";
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

    private String discInfosAsinGet(String asin) {
        String url = "http://" + bcloudIp + ":8762/discInfos/" + asin;
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
