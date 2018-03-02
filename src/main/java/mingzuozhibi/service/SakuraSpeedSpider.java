package mingzuozhibi.service;

import mingzuozhibi.persist.Disc;
import mingzuozhibi.persist.Disc.DiscType;
import mingzuozhibi.persist.Disc.UpdateType;
import mingzuozhibi.persist.DiscList;
import mingzuozhibi.support.Dao;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

import static mingzuozhibi.persist.DiscList.ViewType.SakuraList;
import static mingzuozhibi.service.SakuraSpeedSpider.Util.*;

@Service
public class SakuraSpeedSpider {

    private static final String SAKURA_SPEED_URL = "http://rankstker.net/index_news.cgi";
    private static final Logger LOGGER = LoggerFactory.getLogger(SakuraSpeedSpider.class);

    @Autowired
    private Dao dao;

    public void fetch() throws IOException {
        Document document = Jsoup.connect(SAKURA_SPEED_URL)
                .timeout(30000)
                .get();
        Elements tables = document.select("table");
        Elements fonts = document.select("b>font[color=red]");
        for (int i = 0; i < tables.size(); i++) {
            updateSakura(tables.get(i), fonts.get(i).text());
        }
    }

    private void updateSakura(Element table, String timeText) {
        dao.execute(session -> {
            DiscList discList = getOrCreateSakura(findSakuraKey(table));
            if (timeText.equals("更新中")) {
                LOGGER.info("发现sakura网站更新中");
                return;
            }
            LocalDateTime time = parseTime(timeText);
            if (time.equals(discList.getModifyTime())) {
                LOGGER.debug("不需要更新[{}]列表", discList.getTitle());
                return;
            }
            discList.setEnabled(true);
            discList.setViewType(SakuraList);
            discList.setModifyTime(time);
            updateSakuraDiscs(discList, table.select("tr").stream().skip(1));
        });
    }

    private String findSakuraKey(Element table) {
        return Optional.ofNullable(table.parent().id())
                .filter(key -> !key.isEmpty())
                .orElse("9999-99");
    }

    private DiscList getOrCreateSakura(String key) {
        DiscList discList = dao.lookup(DiscList.class, "key", key);
        if (discList == null) {
            discList = new DiscList(key, null, true, SakuraList);
            dao.save(discList);
            LOGGER.info("发现新的Sakura列表, title={}", discList.getTitle());
        }
        return discList;
    }

    private void updateSakuraDiscs(DiscList discList, Stream<Element> tableRows) {
        LinkedList<Disc> toAdd = new LinkedList<>();
        tableRows.forEach(tr -> {
            String href = tr.child(5).child(0).attr("href");
            String asin = href.substring(href.length() - 10);
            Disc disc = getOrCreateDisc(asin, tr);

            String[] sakuraRank = tr.child(0).text().split("/");
            disc.setThisRank(parseInteger(sakuraRank[0]));
            disc.setPrevRank(parseInteger(sakuraRank[1]));
            disc.setTotalPt(parseInteger(tr.child(2).text()));
            disc.setNicoBook(parseInteger(tr.child(3).text()));
            toAdd.add(disc);
        });
        if ("9999-99".equals(discList.getKey())) {
            discList.setDiscs(toAdd);
        } else {
            List<Disc> sakuraDiscs = discList.getDiscs();
            Set<Disc> discSet = new HashSet<>(sakuraDiscs);
            toAdd.stream().filter(disc -> !discSet.contains(disc))
                    .forEach(sakuraDiscs::add);
        }
        LOGGER.debug("成功更新[{}]列表", discList.getTitle());
    }

    private Disc getOrCreateDisc(String asin, Element tr) {
        Disc disc = dao.lookup(Disc.class, "asin", asin);
        if (disc == null) {
            String title = titleOfDisc(tr.child(5).text());
            String typeIcon = tr.child(1).text();
            String dateText = tr.child(4).text();
            LocalDate releaseDate = parseDate(fixYear(dateText));
            disc = new Disc(asin, title, parseDiscType(typeIcon),
                    UpdateType.Sakura, isAmazonLimit(title), releaseDate);
            disc.setTitlePc(titlePcOfDisc(title));
            LOGGER.info("发现了新的碟片, title={}", disc.getTitle());
        }
        dao.save(disc);
        return disc;
    }

    public static abstract class Util {

        private static final DateTimeFormatter TimeFormatter;
        private static final DateTimeFormatter DateFormatter;

        static {
            TimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 H時m分s秒");
            DateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        }

        public static LocalDate parseDate(String dateText) {
            return LocalDate.parse(dateText, DateFormatter);
        }

        public static LocalDateTime parseTime(String timeText) {
            return LocalDateTime.parse(timeText, TimeFormatter).minusHours(1);
        }

        public static Integer parseInteger(String input) {
            try {
                return Integer.parseInt(input.replaceAll("[^-\\d]+", ""));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        public static String titleOfDisc(String discText) {
            discText = discText.replace("【予約不可】", "");
            discText = discText.replace("【更新停止】", "");
            return discText;
        }

        public static String titlePcOfDisc(String discName) {
            discName = discName.replace("【Blu-ray】", " [Blu-ray]");
            discName = discName.replace("【DVD】", " [DVD]");
            if (isAmazonLimit(discName)) {
                discName = discName.substring(16).trim() + "【尼限定】";
            }
            discName = discName.replaceAll("\\s+", " ");
            return discName;
        }

        public static boolean isAmazonLimit(String japan) {
            return japan.startsWith("【Amazon.co.jp限定】");
        }

        public static String fixYear(String dateText) {
            if (dateText.length() == 8) {
                dateText = "20" + dateText;
            }
            return dateText;
        }

        public static DiscType parseDiscType(String type) {
            switch (type) {
                case "★":
                    return DiscType.Bluray;
                case "○":
                    return DiscType.Dvd;
                case "◎":
                    return DiscType.Box;
                default:
                    return DiscType.Other;
            }
        }
    }
}
