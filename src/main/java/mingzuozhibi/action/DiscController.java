package mingzuozhibi.action;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.Disc.DiscType;
import mingzuozhibi.persist.disc.Disc.UpdateType;
import mingzuozhibi.persist.disc.Record;
import mingzuozhibi.service.amazon.AmazonTaskService;
import mingzuozhibi.support.JsonArg;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static mingzuozhibi.persist.disc.Disc.UpdateType.Both;
import static mingzuozhibi.service.amazon.DocumentReader.getNode;
import static mingzuozhibi.service.amazon.DocumentReader.getText;

@RestController
public class DiscController extends BaseController {

    public static final String COLUMNS = "id,asin,title,titlePc,titleMo," +
            "thisRank,prevRank,nicoBook,totalPt,discType,updateType," +
            "releaseDate,createTime,updateTime,mofidyTime,surplusDays";

    public static final Set<String> COLUMNS_SET = buildSet(COLUMNS);

    public static Set<String> buildSet(String columns) {
        return Stream.of(columns.split(",")).collect(Collectors.toSet());
    }

    @Autowired
    private AmazonTaskService service;

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Transactional
    @GetMapping(value = "/api/discs/{id}", produces = MEDIA_TYPE)
    public String getOne(@PathVariable Long id) {

        Disc disc = dao.get(Disc.class, id);
        if (disc == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[获取碟片失败][指定的碟片Id不存在][Id={}]", id);
            }
            return errorMessage("指定的碟片Id不存在");
        }


        JSONObject result = disc.toJSON(COLUMNS_SET);
        if (LOGGER.isDebugEnabled()) {
            debugRequest("[获取碟片成功][碟片信息={}]", result);
        }
        return objectResult(result);
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @PutMapping(value = "/api/discs/{id}", produces = MEDIA_TYPE)
    public String setOne(@PathVariable Long id,
                         @JsonArg String titlePc,
                         @JsonArg String titleMo,
                         @JsonArg DiscType discType,
                         @JsonArg UpdateType updateType,
                         @JsonArg String releaseDate) {

        if (releaseDate.isEmpty()) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[编辑碟片失败][发售日期不能为空]");
            }
            return errorMessage("发售日期不能为空");
        }

        LocalDate localDate;
        try {
            localDate = LocalDate.parse(releaseDate, formatter);
        } catch (DateTimeParseException e) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[编辑碟片失败][发售日期格式不正确]");
            }
            return errorMessage("发售日期格式不正确");
        }

        Disc disc = dao.get(Disc.class, id);
        if (disc == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[编辑列表失败][指定的碟片Id不存在][Id={}]", id);
            }
            return errorMessage("指定的碟片Id不存在");
        }

        JSONObject before = disc.toJSON(COLUMNS_SET);
        if (LOGGER.isDebugEnabled()) {
            debugRequest("[编辑碟片开始][修改前={}]", before);
        }

        disc.setTitlePc(titlePc);
        disc.setTitleMo(titleMo);
        disc.setDiscType(discType);
        disc.setUpdateType(updateType);
        disc.setReleaseDate(localDate);

        JSONObject result = disc.toJSON(COLUMNS_SET);
        if (LOGGER.isDebugEnabled()) {
            debugRequest("[编辑列表成功][修改后={}]", result);
        }
        return objectResult(result);
    }

    @Transactional
    @PutMapping(value = "/api/discs/{id}/record", produces = MEDIA_TYPE)
    public String setRecord(@PathVariable Long id, @JsonArg String recordText) {
        Disc disc = dao.get(Disc.class, id);
        if (disc == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[更新排名失败][指定的碟片Id不存在][Id={}]", id);
            }
            return errorMessage("指定的碟片Id不存在");
        }

        String regex = "【(\\d{4})年 (\\d{2})月 (\\d{2})日 (\\d{2})時\\(.\\)】 ([*,\\d]{7})位";
        Pattern pattern = Pattern.compile(regex);
        String[] strings = recordText.split("\n");
        int matchLine = 0;
        for (String string : strings) {
            Matcher matcher = pattern.matcher(string);
            if (matcher.find()) {
                Integer rank = getRank(matcher);
                if (rank == null) {
                    continue;
                }
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int date = Integer.parseInt(matcher.group(3));
                int hour = Integer.parseInt(matcher.group(4));
                LocalDate localDate = LocalDate.of(year, month, date);
                Record record = getOrCreateRecord(disc, localDate);
                record.setRank(hour, rank);
                matchLine++;
            }
        }

        @SuppressWarnings("unchecked")
        List<Record> records = dao.create(Record.class)
                .add(Restrictions.eq("disc", disc))
                .add(Restrictions.lt("date", disc.getReleaseDate()))
                .addOrder(Order.asc("date"))
                .list();
        disc.setTotalPt((int) computeTotalPt(disc, records));

        JSONObject result = disc.toJSON(COLUMNS_SET);
        if (LOGGER.isDebugEnabled()) {
            debugRequest("[更新排名成功][共添加记录={}][碟片信息={}]", matchLine, result);
        }
        return objectResult(result);
    }

    private Integer getRank(Matcher matcher) {
        String rankText = matcher.group(5).replaceAll("[*,]", "");
        if (!rankText.isEmpty()) {
            return new Integer(rankText);
        }
        return null;
    }

    private Record getOrCreateRecord(Disc disc, LocalDate localDate) {
        return dao.query(session -> {
            Record record = (Record) session.createCriteria(Record.class)
                    .add(Restrictions.eq("disc", disc))
                    .add(Restrictions.eq("date", localDate))
                    .uniqueResult();
            if (record == null) {
                record = new Record(disc, localDate);
                dao.save(record);
            }
            return record;
        });
    }

    public static double computeTotalPt(Disc disc, List<Record> records) {
        AtomicReference<Integer> lastRank = new AtomicReference<>();
        return records.stream()
                .mapToDouble(record -> computeRecordPt(disc, record, lastRank))
                .sum();
    }

    private static double computeRecordPt(Disc disc, Record record, AtomicReference<Integer> lastRank) {
        double recordPt = 0d;
        for (int i = 0; i < 24; i++) {
            Integer rank = record.getRank(i);
            if (rank == null) {
                rank = lastRank.get();
            } else {
                lastRank.set(rank);
            }
            if (rank != null) {
                recordPt += computeHourPt(disc, rank);
            }
        }
        return recordPt;
    }

    private static double computeHourPt(Disc disc, Integer rank) {
        switch (disc.getDiscType()) {
            case Cd:
                return computePt(150, 5.25, rank);
            case Other:
                return 0d;
            default:
                if (disc.getTitle().contains("Blu-ray")) {
                    if (rank <= 10) {
                        return computePt(100, 3.2, rank);
                    } else if (rank <= 20) {
                        return computePt(100, 3.3, rank);
                    } else if (rank <= 50) {
                        return computePt(100, 3.4, rank);
                    } else if (rank <= 100) {
                        return computePt(100, 3.6, rank);
                    } else if (rank <= 300) {
                        return computePt(100, 3.8, rank);
                    } else {
                        return computePt(100, 3.9, rank);
                    }
                } else {
                    return computePt(100, 4.2, rank);
                }
        }
    }

    private static double computePt(int div, double base, int rank) {
        return div / Math.exp(Math.log(rank) / Math.log(base));
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @GetMapping(value = "/api/discs/search/{asin}", produces = MEDIA_TYPE)
    public String search(@PathVariable String asin) {
        AtomicReference<Disc> disc = new AtomicReference<>(dao.lookup(Disc.class, "asin", asin));
        StringBuilder error = new StringBuilder();
        if (disc.get() == null) {

            Instant instant = Instant.now();
            if (LOGGER.isInfoEnabled()) {
                infoRequest("[查找碟片][从Amazon查询开始][asin={}]", asin);
            }

            service.createDiscTask(asin, task -> {
                if (task.isDone()) {
                    Node node = getNode(task.getDocument(), "Items", "Item", "ItemAttributes");
                    String rankText = getText(task.getDocument(), "Items", "Item", "SalesRank");
                    if (node != null) {
                        Document itemAttributes = node.getOwnerDocument();
                        String title = getText(itemAttributes, "Title");
                        String group = getText(itemAttributes, "ProductGroup");
                        String release = getText(itemAttributes, "ReleaseDate");
                        Objects.requireNonNull(title);
                        Objects.requireNonNull(group);
                        DiscType type = getType(group, title);
                        boolean amazon = title.startsWith("【Amazon.co.jp限定】");
                        LocalDate releaseDate;
                        if (release != null) {
                            releaseDate = LocalDate.parse(release, formatter);
                        } else {
                            releaseDate = LocalDate.now();
                        }
                        Disc newDisc = new Disc(asin, title, type, Both, amazon, releaseDate);
                        if (rankText != null) {
                            newDisc.setThisRank(new Integer(rankText));
                        }
                        dao.save(newDisc);
                        disc.set(newDisc);
                    } else {
                        error.append(task.getErrorMessage());
                    }
                }

                if (LOGGER.isInfoEnabled()) {
                    infoRequest("[查找碟片][从Amazon查询成功][asin={}][耗时={}ms]",
                            asin, Instant.now().toEpochMilli() - instant.toEpochMilli());
                }

                synchronized (disc) {
                    disc.notify();
                }
            });

            try {
                synchronized (disc) {
                    TimeUnit.SECONDS.timedWait(disc, 20);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();

            }
        }
        if (disc.get() == null) {
            if (error.length() == 0) {

                if (LOGGER.isInfoEnabled()) {
                    infoRequest("[查找碟片][从Amazon查询超时][asin={}]]", asin);
                }

                return errorMessage("查询超时，你可以稍后再尝试");
            } else {
                return errorMessage(error.toString());
            }
        }

        JSONArray result = new JSONArray();
        JSONObject discJSON = disc.get().toJSON(COLUMNS_SET);
        if (LOGGER.isInfoEnabled()) {
            infoRequest("[查找碟片成功][碟片信息={}]", discJSON);
        }
        result.put(discJSON);
        return objectResult(result);
    }

    private DiscType getType(String group, String title) {
        switch (group) {
            case "Music":
                return DiscType.Cd;
            case "DVD":
                if (title.contains("Blu-ray")) {
                    return DiscType.Bluray;
                } else {
                    return DiscType.Dvd;
                }
            default:
                return DiscType.Other;
        }
    }

}
