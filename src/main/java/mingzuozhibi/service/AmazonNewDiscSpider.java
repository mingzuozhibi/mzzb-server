package mingzuozhibi.service;

import io.webfolder.cdp.Launcher;
import io.webfolder.cdp.session.Session;
import io.webfolder.cdp.session.SessionFactory;
import io.webfolder.cdp.session.WaitUntil;
import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.DiscInfo;
import mingzuozhibi.support.Dao;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class AmazonNewDiscSpider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmazonNewDiscSpider.class);

    @Autowired
    private Dao dao;

    @Transactional
    public void fetch() {
        new Thread(() -> {

            killChrome();

            List<String> command = new ArrayList<>();
            command.add("--headless");
            Launcher launcher = new Launcher();
            try (SessionFactory factory = launcher.launch(command)) {
                fetchPage(factory, 60, "https://www.amazon.co.jp/s/ref=sr_pg_1?rh=n%3A561958%2Cn%3A%21562002%2Cn%3A562020&sort=date-desc-rank&ie=UTF8");
                fetchPage(factory, 10, "https://www.amazon.co.jp/s/ref=sr_pg_1?rh=n%3A561958%2Cn%3A%21562002%2Cn%3A562026%2Cn%3A2201429051&sort=date-desc-rank&&ie=UTF8");
            } finally {

                killChrome();

            }
        }).start();
    }

    private void killChrome() {
        try {
            Runtime.getRuntime().exec("ps -ef | grep chrome | awk '{print $2}' | xargs -t -i kill -9 {}");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            threadSleep(5);
        }
    }

    private void fetchPage(SessionFactory factory, int maxPage, String baseUrl) {
        for (int page = 1; page <= maxPage; page++) {
            LOGGER.info("扫描新碟片中({}/{})", page, maxPage);

            String url = baseUrl + "&page=" + page;
            for (int retry = 1; retry <= 3; retry++) {
                try (Session session = factory.create()) {
                    session.setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36");
                    session.navigateAndWait(url, WaitUntil.DomReady, 60000);
                    LOGGER.debug("Dom已就绪({}/{})", page, maxPage);
                        Document document = Jsoup.parse(session.getOuterHtml("body"));
                        Elements elements = document.select("#s-results-list-atf > li");

                        LOGGER.debug("发现{}个结果({}/{})", elements.size(), page, maxPage);
                        if (elements.size() > 0) {
                            elements.forEach(element -> {
                                String asin = element.attr("data-asin");
                                if (asin != null && asin.length() > 0) {
                                    DiscInfo discInfo = dao.lookup(DiscInfo.class, "asin", asin);
                                    if (discInfo == null) {
                                        String title = element.select("a.a-link-normal").attr("title");
                                        dao.save(createDiscInfo(asin, title));
                                        if (LOGGER.isDebugEnabled()) {
                                            LOGGER.debug("[发现新碟片][ASIN={}][title={}]", asin, title);
                                        }
                                    }
                                }
                            });
                            break;
                        }
                        LOGGER.debug(String.format("[扫描新碟片数据异常][retry=%d/3][size=%d]", retry, elements.size()));
                } catch (RuntimeException e) {
                    LOGGER.debug(String.format("[扫描新碟片遇到错误][retry=%d/3]", retry), e);
                }
            }
            threadSleep(5);
        }
    }

    private DiscInfo createDiscInfo(String asin, String title) {
        DiscInfo discInfo;
        discInfo = new DiscInfo(asin, title);
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
