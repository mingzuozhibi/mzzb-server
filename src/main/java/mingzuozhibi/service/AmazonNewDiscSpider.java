package mingzuozhibi.service;

import io.webfolder.cdp.Launcher;
import io.webfolder.cdp.session.Session;
import io.webfolder.cdp.session.SessionFactory;
import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.DiscInfo;
import mingzuozhibi.support.Dao;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class AmazonNewDiscSpider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmazonNewDiscSpider.class);

    @Autowired
    private Dao dao;

    public void fetch() {
        new Thread(() -> {

            killChrome();

            List<String> command = new ArrayList<>();
            command.add("--headless");
            Launcher launcher = new Launcher();
            try (SessionFactory factory = launcher.launch(command)) {
                fetchPage(factory, 60, "https://www.amazon.co.jp/s/ref=sr_pg_1?rh=n%3A561958%2Cn%3A%21562002%2Cn%3A562020&sort=date-desc-rank&ie=UTF8");
                fetchPage(factory, 10, "https://www.amazon.co.jp/s/ref=sr_pg_1?rh=n%3A561958%2Cn%3A%21562002%2Cn%3A562026%2Cn%3A2201429051&sort=date-desc-rank&&ie=UTF8");
            }
        }).start();
    }

    private void killChrome() {
        try {
            Process exec = Runtime.getRuntime().exec("ps -xf | grep google-chrome | awk '{print $1}' | xargs kill -9");
            String output = IOUtils.toString(exec.getInputStream(), Charset.defaultCharset());
            LOGGER.info("KillChrome: {}", output);
        } catch (IOException e) {
            LOGGER.info("KillChrome: " + e.getMessage(), e);
        } finally {
            threadSleep(10);
        }
    }

    public void fetchFromJapan(String japanServerIp) {
        new Thread(() -> {
            int page = 0;

            MAIN_LOOP:
            while (true) {
                LOGGER.info("扫描新碟片中({}/{})", page + 1, 20);
                for (int retry = 1; retry <= 3; retry++) {
                    try {
                        String body = Jsoup.connect("http://" + japanServerIp + ":9090/api/newdiscs?page=" + page)
                                .ignoreContentType(true)
                                .timeout(10000)
                                .execute()
                                .body();
                        JSONObject root = new JSONObject(body);
                        JSONObject data = root.getJSONObject("data");

                        JSONArray newdiscs = data.getJSONArray("newdiscs");
                        for (int i = 0; i < newdiscs.length(); i++) {
                            JSONObject newdisc = newdiscs.getJSONObject(i);
                            tryCreateDiscInfo(newdisc.getString("asin"), newdisc.getString("title"));
                        }

                        JSONObject pageInfo = data.getJSONObject("pageInfo");
                        if (++page > pageInfo.getInt("maxPage") || page >= 20) {
                            break MAIN_LOOP;
                        } else {
                            break;
                        }
                    } catch (IOException e) {
                        LOGGER.debug(String.format("[扫描新碟片遇到错误][retry=%d/3][message=%s]", retry, e.getMessage()), e);
                    }
                }
            }
        }).start();
    }

    private void fetchPage(SessionFactory factory, int maxPage, String baseUrl) {
        int error = 0;
        for (int page = 1; page <= maxPage; page++) {
            LOGGER.info("扫描新碟片中({}/{})", page, maxPage);

            String url = baseUrl + "&page=" + page;
            for (int retry = 1; retry <= 3; retry++) {
                try (Session session = factory.create()) {
                    session.setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36");
                    session.navigate(url);
                    session.waitDocumentReady(38000);
                    session.wait(2000);

                    Document document = Jsoup.parse(session.getOuterHtml("body"));
                    Elements elements = document.select("#s-results-list-atf > li[data-result-rank]");

                    LOGGER.info("发现{}个结果({}/{})", elements.size(), page, maxPage);
                    if (elements.size() > 0) {
                        elements.forEach(element -> {
                            String asin = element.attr("data-asin");
                            String title = element.select("a.a-link-normal").attr("title");
                            tryCreateDiscInfo(asin, title);
                        });
                        error = 0;
                        break;
                    }
                    String outerHtml = document.outerHtml();
                    if (outerHtml.contains("api-services-support@amazon.com")) {
                        LOGGER.error("[扫描新碟片数据异常][侦测到Amazon反发爬虫系统]");
                        return;
                    }
                    if (++error > 10) {
                        LOGGER.error("[扫描新碟片数据异常][连续10次没有获取到数据][document={}]", document.outerHtml());
                        return;
                    }
                    LOGGER.debug(String.format("[扫描新碟片数据异常][retry=%d/3][size=%d]", retry, elements.size()));
                } catch (RuntimeException e) {
                    LOGGER.debug(String.format("[扫描新碟片遇到错误][retry=%d/3][message=%s]", retry, e.getMessage()), e);
                }
            }
            threadSleep(30);
        }
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

    @Transactional
    public void updateNewDiscFollowd(Disc disc) {
        Optional.ofNullable(dao.lookup(DiscInfo.class, "asin", disc.getAsin()))
                .ifPresent(discInfo -> {
                    discInfo.setFollowed(true);
                });
    }

    private void threadSleep(int timeout) {
        try {
            TimeUnit.SECONDS.sleep(timeout);
        } catch (InterruptedException ignored) {
        }
    }

}
