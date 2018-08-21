package mingzuozhibi.action;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.Disc.DiscType;
import mingzuozhibi.persist.disc.Disc.UpdateType;
import mingzuozhibi.service.AmazonNewDiscSpider;
import mingzuozhibi.service.amazon.AmazonTaskService;
import mingzuozhibi.support.JsonArg;
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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static mingzuozhibi.service.amazon.DocumentReader.getNode;
import static mingzuozhibi.service.amazon.DocumentReader.getText;
import static mingzuozhibi.support.SakuraHelper.*;

@RestController
public class DiscController extends BaseController {

    public static Set<String> buildSet(String columns) {
        return Stream.of(columns.split(",")).collect(Collectors.toSet());
    }

    @Autowired
    private AmazonTaskService service;

    @Autowired
    private AmazonNewDiscSpider amazonNewDiscSpider;

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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

        JSONObject result = disc.toJSON();

        if (LOGGER.isDebugEnabled()) {
            debugRequest("[获取碟片成功][碟片信息={}]", result);
        }

        result.put("ranks", buildRanks(dao, disc));

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
                warnRequest("[编辑碟片失败][指定的碟片Id不存在][Id={}]", id);
            }
            return errorMessage("指定的碟片Id不存在");
        }

        JSONObject before = disc.toJSON();
        if (LOGGER.isDebugEnabled()) {
            debugRequest("[编辑碟片开始][修改前={}]", before);
        }

        disc.setTitlePc(titlePc);
        disc.setTitleMo(titleMo);
        disc.setDiscType(discType);
        disc.setUpdateType(updateType);
        disc.setReleaseDate(localDate);

        JSONObject result = disc.toJSON();

        if (LOGGER.isDebugEnabled()) {
            debugRequest("[编辑碟片成功][修改后={}]", result);
        }

        result.put("ranks", buildRanks(dao, disc));

        return objectResult(result);
    }

    @Transactional
    @GetMapping(value = "/api/discs/{id}/records", produces = MEDIA_TYPE)
    public String findRecords(@PathVariable Long id) {
        Disc disc = dao.get(Disc.class, id);
        if (disc == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[获取碟片排名失败][指定的碟片Id不存在][Id={}]", id);
            }
            return errorMessage("指定的碟片Id不存在");
        }

        JSONObject result = disc.toJSON();

        if (LOGGER.isDebugEnabled()) {
            debugRequest("[获取碟片排名成功][碟片信息={}]", result);
        }

        result.put("records", buildRecords(dao, disc));

        return objectResult(result);
    }

    @Transactional
    @PutMapping(value = "/api/discs/{id}/ranks", produces = MEDIA_TYPE)
    public String mergeRanks(@PathVariable Long id, @JsonArg String text) {
        Disc disc = dao.get(Disc.class, id);
        if (disc == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[提交排名失败][指定的碟片Id不存在][Id={}]", id);
            }
            return errorMessage("指定的碟片Id不存在");
        }

        int matchLine = mergeRankText(dao, disc, text);

        computeAndUpdateAmazonPt(dao, disc);

        JSONObject result = disc.toJSON();

        if (LOGGER.isDebugEnabled()) {
            debugRequest("[提交排名成功][提交记录数={}][碟片信息={}]", matchLine, result);
        }

        result.put("records", buildRecords(dao, disc));

        return objectResult(result);
    }

    @Transactional
    @PutMapping(value = "/api/discs/{id}/pts", produces = MEDIA_TYPE)
    public String mergePts(@PathVariable Long id, @JsonArg String text) {
        Disc disc = dao.get(Disc.class, id);
        if (disc == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[提交PT失败][指定的碟片Id不存在][Id={}]", id);
            }
            return errorMessage("指定的碟片Id不存在");
        }

        int matchLine = mergePtText(dao, disc, text);

        computeAndUpdateSakuraPt(dao, disc);

        JSONObject result = disc.toJSON();

        if (LOGGER.isDebugEnabled()) {
            debugRequest("[提交PT成功][提交记录数={}][碟片信息={}]", matchLine, result);
        }

        result.put("records", buildRecords(dao, disc));

        return objectResult(result);
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @GetMapping(value = "/api/discs/search/{asin}", produces = MEDIA_TYPE)
    public String search(@PathVariable String asin) {
        AtomicReference<Disc> disc = new AtomicReference<>(dao.lookup(Disc.class, "asin", asin));
        StringBuffer error = new StringBuffer();
        if (disc.get() == null) {
            searchFromAmazon(asin, disc, error);
            waitForSearch(disc);
        }

        if (disc.get() == null) {
            if (error.length() > 0) {
                return errorMessage(error.toString());
            }
            if (LOGGER.isInfoEnabled()) {
                infoRequest("[查找碟片][从Amazon查询超时][asin={}]]", asin);
            }
            return errorMessage("查询超时，你可以稍后再尝试");
        }

        JSONArray result = new JSONArray();
        JSONObject discJSON = disc.get().toJSON();

        amazonNewDiscSpider.updateNewDiscFollowd(disc.get());

        if (LOGGER.isInfoEnabled()) {
            infoRequest("[查找碟片成功][碟片信息={}]", discJSON);
        }
        result.put(discJSON);
        return objectResult(result);
    }

    private void waitForSearch(AtomicReference<Disc> disc) {
        try {
            synchronized (disc) {
                TimeUnit.SECONDS.timedWait(disc, 20);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void searchFromAmazon(String asin, AtomicReference<Disc> disc, StringBuffer error) {
        Instant instant = Instant.now();
        if (LOGGER.isInfoEnabled()) {
            infoRequest("[查找碟片][从Amazon查询开始][asin={}]", asin);
        }

        service.createFindTask(asin, task -> {
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
                    title = formatTitle(title);
                    DiscType type = getType(group, title);
                    boolean amazon = title.startsWith("【Amazon.co.jp限定】");
                    LocalDate releaseDate = getReleaseDate(release);
                    Disc newDisc = new Disc(asin, title, type, UpdateType.Both, amazon, releaseDate);
                    if (rankText != null) {
                        newDisc.setThisRank(new Integer(rankText));
                    }
                    dao.save(newDisc);
                    disc.set(newDisc);
                } else {
                    error.append(getText(task.getDocument(), "Items", "Request", "Errors", "Message"));
                }
            } else {
                error.append(task.getErrorMessage());
            }

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("[查找碟片][从Amazon查询成功][asin={}][耗时={}ms]",
                        asin, Instant.now().toEpochMilli() - instant.toEpochMilli());
            }

            synchronized (disc) {
                disc.notify();
            }
        });
    }

    public static LocalDate getReleaseDate(String release) {
        if (release != null) {
            return LocalDate.parse(release, formatter);
        } else {
            return LocalDate.now();
        }
    }

    public static String formatTitle(String title) {
        title = decodeAmazonText(title);
        if (title.length() > 500) {
            title = title.substring(0, 500);
        }
        return title;
    }

    private static Pattern pattern = Pattern.compile("&#x([0-9a-f]{4});");

    private static String decodeAmazonText(String input) {
        StringBuffer buffer = new StringBuffer();
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            matcher.appendReplacement(buffer, decodeHex(matcher));
        }
        return matcher.appendTail(buffer).toString();
    }

    private static String decodeHex(Matcher matcher) {
        return new String(Character.toChars(Integer.parseInt(matcher.group(1), 16)));
    }

    private static DiscType getType(String group, String title) {
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
