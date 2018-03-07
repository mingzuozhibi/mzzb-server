package mingzuozhibi.service;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.Disc.DiscType;
import mingzuozhibi.persist.disc.Disc.UpdateType;
import mingzuozhibi.persist.disc.Record;
import mingzuozhibi.persist.disc.Sakura;
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

import static mingzuozhibi.persist.disc.Sakura.ViewType.SakuraList;
import static mingzuozhibi.service.RecordHelper.getOrCreateRecord;
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
            Sakura sakura = getOrCreateSakura(findSakuraKey(table));
            if (timeText.equals("更新中")) {
                LOGGER.info("发现sakura网站更新中");
                return;
            }
            LocalDateTime time = parseTime(timeText);
            if (time.equals(sakura.getModifyTime())) {
                LOGGER.debug("不需要更新[{}]列表", sakura.getTitle());
                return;
            }
            sakura.setEnabled(true);
            sakura.setViewType(SakuraList);
            sakura.setModifyTime(time);
            updateSakuraDiscs(sakura, table.select("tr").stream().skip(1));
        });
    }

    private String findSakuraKey(Element table) {
        return Optional.ofNullable(table.parent().id())
                .filter(key -> !key.isEmpty())
                .orElse("9999-99");
    }

    private Sakura getOrCreateSakura(String key) {
        Sakura sakura = dao.lookup(Sakura.class, "key", key);
        if (sakura == null) {
            sakura = new Sakura(key, null, true, SakuraList);
            dao.save(sakura);
            LOGGER.info("发现新的Sakura列表, title={}", sakura.getTitle());
        }
        return sakura;
    }

    private void updateSakuraDiscs(Sakura sakura, Stream<Element> tableRows) {
        LocalDate recordDate = sakura.getModifyTime().plusHours(1).toLocalDate();
        int recordHour = sakura.getModifyTime().plusHours(1).getHour();
        List<Disc> toAdd = new ArrayList<>(sakura.getDiscs().size());
        boolean isTop100 = "9999-99".equals(sakura.getKey());

        tableRows.forEach(tr -> {
            String href = tr.child(5).child(0).attr("href");
            String asin = href.substring(href.length() - 10);
            Disc disc = getOrCreateDisc(asin, tr);

            if (disc.getUpdateType() == UpdateType.Both && !isTop100) {
                disc.setUpdateType(UpdateType.Sakura);
            }
            if (disc.getUpdateType() == UpdateType.Sakura || disc.getUpdateType() == UpdateType.Both) {
                String[] sakuraRank = tr.child(0).text().split("/");
                disc.setThisRank(parseInteger(sakuraRank[0]));
                disc.setPrevRank(parseInteger(sakuraRank[1]));
                disc.setTotalPt(parseInteger(tr.child(2).text()));
                disc.setNicoBook(parseInteger(tr.child(3).text()));

                disc.setUpdateTime(sakura.getModifyTime());
                if (!Objects.equals(disc.getThisRank(), disc.getPrevRank())) {
                    disc.setModifyTime(sakura.getModifyTime());
                }

                if (!isTop100) {
                    Record record = getOrCreateRecord(dao, disc, recordDate);
                    record.setRank(recordHour, disc.getThisRank());
                    record.setTotalPt(disc.getTotalPt());
                }
            }
            toAdd.add(disc);
        });
        if (isTop100) {
            sakura.setDiscs(toAdd);
        } else {
            sakura.getDiscs().stream()
                    .filter(disc -> disc.getUpdateType() != UpdateType.Sakura)
                    .forEach(toAdd::add);
            toAdd.sort(Comparator.naturalOrder());
            sakura.setDiscs(toAdd);
        }
        LOGGER.debug("成功更新[{}]列表", sakura.getTitle());
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
            dao.save(disc);
            LOGGER.info("发现了新的碟片, title={}", disc.getTitle());
        }
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
