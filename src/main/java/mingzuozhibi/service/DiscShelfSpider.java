package mingzuozhibi.service;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.DiscShelf;
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
public class DiscShelfSpider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscShelfSpider.class);

    @Autowired
    private Dao dao;

    public void fetchFromJapan(String japanServerIp) {
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
                        JSONObject obj = data.getJSONObject(i);
                        tryCreateDiscShelf(obj.getString("asin"), obj.getString("title"));
                    }

                    break;
                } catch (IOException e) {
                    LOGGER.debug(String.format("[扫描新碟片遇到错误][retry=%d/3][message=%s]", retry, e.getMessage()), e);
                }
            }
        }
    }

    private void tryCreateDiscShelf(String asin, String title) {
        if (asin != null && asin.length() > 0) {
            DiscShelf discShelf = dao.lookup(DiscShelf.class, "asin", asin);
            if (discShelf == null) {
                dao.save(createDiscShelf(asin, title));
                LOGGER.info("[发现新碟片][asin={}][title={}]", asin, title);
            }
        }
    }

    private DiscShelf createDiscShelf(String asin, String title) {
        DiscShelf discShelf = new DiscShelf(asin, title);
        if (dao.lookup(Disc.class, "asin", asin) != null) {
            discShelf.setFollowed(true);
        }
        return discShelf;
    }

}
