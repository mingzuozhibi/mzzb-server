package mingzuozhibi.service;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.DiscShelf;
import mingzuozhibi.support.Dao;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DiscShelfSpider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscShelfSpider.class);

    @Autowired
    private SpiderHelper spiderHelper;

    @Autowired
    private Dao dao;

    public void fetchFromBCloud() {
        for (int page = 1; page <= 20; page++) {
            LOGGER.info("扫描新碟片中({}/{})", page, 20);
            String url = spiderHelper.discShelfs("/discShelfs?page=%d", page);
            JSONObject root = new JSONObject(spiderHelper.waitRequest(url));
            for (Object object : root.getJSONArray("data")) {
                createOrUpdate((JSONObject) object);
            }
        }
    }

    private void createOrUpdate(JSONObject object) {
        String asin = object.getString("asin");
        String title = object.getString("title");
        if (StringUtils.isNotEmpty(asin)) {
            dao.execute(session -> {
                createOrUpdate(asin, title);
            });
        }
    }

    private void createOrUpdate(String asin, String title) {
        DiscShelf discShelf = dao.lookup(DiscShelf.class, "asin", asin);
        if (discShelf == null) {
            discShelf = new DiscShelf(asin, title, discExists(asin));
            dao.save(discShelf);
            LOGGER.info("[发现新碟片][asin={}][title={}]", asin, title);
        } else {
            discShelf.setTitle(title);
        }
    }

    private boolean discExists(String asin) {
        return dao.lookup(Disc.class, "asin", asin) != null;
    }

}
