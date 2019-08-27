package mingzuozhibi.service;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.DiscInfo;
import mingzuozhibi.support.Dao;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class AmazonNewDiscSpider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmazonNewDiscSpider.class);

    @Autowired
    private Dao dao;

    public void fetchFromJapan(String japanServerIp) {
        new Thread(() -> {
            for (int p = 0; p < 20; p++) {
                LOGGER.info("扫描新碟片中({}/{})", p + 1, 20);
                for (int retry = 1; retry <= 3; retry++) {
                    try {
                        String body = Jsoup.connect(String.format("http://%s:8762/newDiscs?page=%d&sort=id,desc", japanServerIp, p))
                                .ignoreContentType(true)
                                .timeout(10000)
                                .execute()
                                .body();
                        JSONObject root = new JSONObject(body);

                        JSONArray data = root.getJSONArray("data");
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject newdisc = data.getJSONObject(i);
                            tryCreateDiscInfo(newdisc.getString("asin"), newdisc.getString("title"));
                        }

                        break;
                    } catch (IOException e) {
                        LOGGER.debug(String.format("[扫描新碟片遇到错误][retry=%d/3][message=%s]", retry, e.getMessage()), e);
                    }
                }
            }
        }).start();
    }

    private void tryCreateDiscInfo(String asin, String title) {
        if (asin != null && asin.length() > 0) {
            DiscInfo discInfo = dao.lookup(DiscInfo.class, "asin", asin);
            if (discInfo == null) {
                dao.save(createDiscInfo(asin, title));
                LOGGER.info("[发现新碟片][asin={}][title={}]", asin, title);
            }
        }
    }

    private DiscInfo createDiscInfo(String asin, String title) {
        DiscInfo discInfo = new DiscInfo(asin, title);
        if (dao.lookup(Disc.class, "asin", asin) != null) {
            discInfo.setFollowed(true);
        }
        return discInfo;
    }

}
