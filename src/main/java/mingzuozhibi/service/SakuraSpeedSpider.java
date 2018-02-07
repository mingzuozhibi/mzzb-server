package mingzuozhibi.service;

import mingzuozhibi.persist.Disc;
import mingzuozhibi.persist.Disc.DiscType;
import mingzuozhibi.persist.Disc.UpdateType;
import mingzuozhibi.persist.Sakura;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Service
public class SakuraSpeedSpider {

    private static final String SAKURA_SPEED_URL = "http://rankstker.net/index_news.cgi";

    private static final DateTimeFormatter update = DateTimeFormatter.ofPattern("yyyy年M月d日 H時m分s秒")
            .withZone(ZoneId.of("Asia/Tokyo"));
    private static final DateTimeFormatter release = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
            .withZone(ZoneId.of("Asia/Tokyo"));

    @Autowired
    private Dao dao;

    public void fetch() throws IOException {
        Document document = Jsoup.connect(SAKURA_SPEED_URL).get();
        Elements tables = document.select("table");
        Elements fonts = document.select("b>font[color=red]");
        for (int i = 0; i < tables.size(); i++) {
            updateSakura(tables.get(i), fonts.get(i).text());
        }
    }

    private void updateSakura(Element table, String updateText) {
        dao.execute(session -> {
            Sakura sakura = getOrCreateSakura(table.parent().id());
            if (!updateText.equals("更新中")) {
                updateSakura(table, sakura, parseTime(update, updateText));
            }
        });
    }

    private Sakura getOrCreateSakura(String key) {
        String searchKey = key.isEmpty() ? Sakura.TOP100 : key;
        Sakura sakura = dao.lookup(Sakura.class, "key", searchKey);
        if (sakura == null) {
            Logger logger = LoggerFactory.getLogger(SakuraSpeedSpider.class);
            logger.info("发现新的Sakura列表, key={}", key);
            sakura = new Sakura(key, null);
            dao.save(sakura);
        }
        return sakura;
    }

    private void updateSakura(Element table, Sakura sakura, LocalDateTime updateTime) {
        LinkedList<Disc> toAdd = new LinkedList<>();
        table.select("tr").stream().skip(1).forEach(tr -> {
            String href = tr.child(5).child(0).attr("href");
            String asin = href.substring(href.length() - 10);
            Disc disc = getOrCreateDisc(asin, tr);

            String[] sakuraRank = tr.child(0).text().split("/");
            disc.setThisRank(parseNumber(sakuraRank[0]));
            disc.setPrevRank(parseNumber(sakuraRank[1]));
            disc.setTotalPt(parseNumber(tr.child(2).text()));
            disc.setNicoBook(parseNumber(tr.child(3).text()));
            toAdd.add(disc);
        });
        sakura.setSakuraUpdateDate(updateTime);
        if (sakura.isTop100()) {
            sakura.setDiscs(toAdd);
        } else {
            List<Disc> sakuraDiscs = sakura.getDiscs();
            Set<Disc> discSet = new HashSet<>(sakuraDiscs);
            toAdd.stream().filter(disc -> !discSet.contains(disc))
                    .forEach(sakuraDiscs::add);
        }
    }

    private Disc getOrCreateDisc(String asin, Element tr) {
        Disc disc = dao.lookup(Disc.class, "asin", asin);
        if (disc == null) {
            String title = nameOfDisc(tr.child(5).text());
            String typeIcon = tr.child(1).text();
            disc = new Disc(asin, title, parseDiscType(typeIcon),
                    UpdateType.Sakura, isAmazonLimit(title), parseRelease(tr));
            disc.setTitlePc(titleOfDisc(title));
        }
        dao.save(disc);
        return disc;
    }

    private DiscType parseDiscType(String type) {
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

    private LocalDate parseRelease(Element tr) {
        String dateText = tr.child(4).text();
        if (dateText.length() == 8) {
            dateText = "20" + dateText;
        }
        return parseDate(release, dateText);
    }

    private String nameOfDisc(String discText) {
        discText = discText.replace("【予約不可】", "");
        discText = discText.replace("【更新停止】", "");
        return discText;
    }

    public static String titleOfDisc(String discName) {
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

    private LocalDateTime parseTime(DateTimeFormatter formatter, String text) {
        return ZonedDateTime.parse(text, formatter).toLocalDateTime();
    }

    private LocalDate parseDate(DateTimeFormatter formatter, String text) {
        return ZonedDateTime.parse(text + " 09:00:00", formatter).toLocalDate();
    }

    private int parseNumber(String number) {
        int result = 0;
        for (int i = 0; i < number.length(); i++) {
            char ch = number.charAt(i);
            if (Character.isDigit(ch)) {
                result *= 10;
                result += ch - '0';
            }
        }
        return result;
    }

}
