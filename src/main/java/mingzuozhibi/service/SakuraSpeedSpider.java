package mingzuozhibi.service;

import mingzuozhibi.persist.model.DiscType;
import mingzuozhibi.persist.model.disc.Disc;
import mingzuozhibi.persist.model.disc.DiscRepository;
import mingzuozhibi.persist.model.discList.DiscList;
import mingzuozhibi.persist.model.discList.DiscListRepository;
import mingzuozhibi.persist.model.discSakura.DiscSakura;
import mingzuozhibi.persist.model.discSakura.DiscSakuraRepository;
import mingzuozhibi.support.Dao;
import org.apache.commons.lang3.time.DateUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import static mingzuozhibi.persist.model.disc.Disc.isAmzver;
import static mingzuozhibi.persist.model.disc.Disc.titleOfDisc;
import static mingzuozhibi.persist.model.discList.DiscList.titleOfSeason;

@Service
public class SakuraSpeedSpider {

    private static final String SAKURA_SPEED_URL = "http://rankstker.net/index_news.cgi";

    private SimpleDateFormat update = new SimpleDateFormat("yyyy年M月d日 H時m分s秒");
    private SimpleDateFormat release = new SimpleDateFormat("yyyy/MM/dd");

    @Autowired
    private Dao dao;

    @Autowired
    private DiscRepository discRepository;

    @Autowired
    private DiscListRepository discListRepository;

    @Autowired
    private DiscSakuraRepository discSakuraRepository;

    public boolean timeout() {
        return true;
    }

    public void fetch() throws IOException {
        Document document = Jsoup.connect(SAKURA_SPEED_URL).get();
        Elements tables = document.select("table");
        Elements fonts = document.select("b>font[color=red]");
        for (int i = 0; i < tables.size(); i++) {
            updateDiscList(tables.get(i), fonts.get(i).text());
        }
    }

    private void updateDiscList(Element table, String updateText) {
        dao.execute(session -> {
            DiscList discList = getDiscList(table.parent().id());
            if (!updateText.equals("更新中")) {
                Date japanDate = parseDate(update, updateText);
                Date chinaDate = DateUtils.addHours(japanDate, -1);
                updateDiscList(table, discList, chinaDate);
            }
        });
    }

    private void updateDiscList(Element table, DiscList discList, Date updateTime) {
        LinkedList<Disc> discs = new LinkedList<>();
        table.select("tr").stream().skip(1).forEach(tr -> {
            String href = tr.child(5).child(0).attr("href");
            String asin = href.substring(href.length() - 10);
            Disc disc = getDisc(asin, tr);

            DiscSakura discSakura = getDiscSakura(disc);
            String[] sakuraRank = tr.child(0).text().split("/");
            discSakura.setCurk(parseNumber(sakuraRank[0]));
            discSakura.setPrrk(parseNumber(sakuraRank[1]));
            discSakura.setCupt(parseNumber(tr.child(2).text()));
            discSakura.setCubk(parseNumber(tr.child(3).text()));
            discSakura.setSday(getSday(disc));
            discSakura.setDate(updateTime);
            discSakuraRepository.save(discSakura);
            discs.add(disc);
        });
        discList.setDate(updateTime);
        if (discList.isTop100()) {
            discList.setDiscs(discs);
            discListRepository.save(discList);
        } else {
            Set<Disc> discSet = new HashSet<>(discList.getDiscs());
            discs.stream()
                    .filter(disc -> !discSet.contains(disc))
                    .forEach(discList.getDiscs()::add);
            discListRepository.save(discList);
        }
    }

    private Disc getDisc(String asin, Element tr) {
        Disc disc = discRepository.findByAsin(asin);
        if (disc == null) {
            String japan = nameOfDisc(tr.child(5).text());
            String type = tr.child(1).text();

            disc = new Disc();
            disc.setAsin(asin);
            disc.setJapan(japan);
            disc.setTitle(titleOfDisc(japan));
            disc.setRelease(parseRelease(tr));
            disc.setAmzver(isAmzver(japan));
            if (type.equals("◎")) {
                if (japan.contains("Blu-ray")) {
                    disc.setType(DiscType.BD_BOX);
                } else {
                    disc.setType(DiscType.DVD_BOX);
                }
            } else {
                if (japan.contains("Blu-ray")) {
                    disc.setType(DiscType.BD);
                } else {
                    disc.setType(DiscType.DVD);
                }
            }
        }
        discRepository.save(disc);
        return disc;
    }

    private Date parseRelease(Element tr) {
        String dateText = tr.child(4).text();
        if (dateText.length() == 8) {
            dateText = "20" + dateText;
        }
        try {
            return parseDate(release, dateText);
        } catch (RuntimeException e) {
            return new Date();
        }
    }

    private DiscList getDiscList(String name) {
        if (name == null || name.isEmpty()) {
            return getDiscList("top_100", "日亚实时TOP100");
        } else {
            return getDiscList(name, titleOfSeason(name));
        }
    }

    private DiscList getDiscList(String name, String title) {
        DiscList discList = discListRepository.findByName(name);
        if (discList == null) {
            discList = new DiscList();
            discList.setName(name);
            discList.setTitle(title);
            discList.setSakura(true);
            discListRepository.save(discList);
        }
        return discList;
    }

    private DiscSakura getDiscSakura(Disc disc) {
        DiscSakura discSakura = disc.getSakura();
        if (discSakura == null) {
            discSakura = new DiscSakura();
            discSakura.setDisc(disc);
        }
        return discSakura;
    }

    private String nameOfDisc(String discText) {
        discText = discText.replace("【予約不可】", "");
        discText = discText.replace("【更新停止】", "");
        return discText;
    }

    private Date parseDate(SimpleDateFormat dateFormat, String dateText) {
        try {
            return dateFormat.parse(dateText);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
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

    private int getSday(Disc disc) {
        long currentTime = System.currentTimeMillis();
        long releaseTime = disc.getRelease().getTime() - 3600000L;
        return (int) Math.floorDiv(releaseTime - currentTime, 86400000L);
    }

}
