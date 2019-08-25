package mingzuozhibi.action;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.Disc.DiscType;
import mingzuozhibi.persist.disc.Disc.UpdateType;
import mingzuozhibi.service.AmazonDiscSpider;
import mingzuozhibi.service.AmazonNewDiscSpider;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static mingzuozhibi.utils.DiscUtils.needUpdateAsins;
import static mingzuozhibi.utils.RecordUtils.buildRanks;
import static mingzuozhibi.utils.RecordUtils.buildRecords;

@RestController
public class DiscController extends BaseController {

    public static Set<String> buildSet(String columns) {
        return Stream.of(columns.split(",")).collect(Collectors.toSet());
    }

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Autowired
    private AmazonNewDiscSpider amazonNewDiscSpider;

    @Autowired
    private AmazonDiscSpider amazonDiscSpider;

    @Transactional
    @GetMapping(value = "/api/discs/activeCount")
    public String activeCount() {
        return objectResult(needUpdateAsins(dao.session()).size());
    }

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

        return responseDisc(disc);
    }

    private String responseDisc(Disc disc) {
        JSONObject result = disc.toJSON();

        if (LOGGER.isDebugEnabled()) {
            debugRequest("[获取碟片成功][碟片信息={}]", result);
        }

        result.put("ranks", buildRanks(dao, disc));

        return objectResult(result);
    }

    @Transactional
    @GetMapping(value = "/api/discs/asin/{asin}", produces = MEDIA_TYPE)
    public String getOne(@PathVariable String asin) {

        Disc disc = dao.lookup(Disc.class, "asin", asin);
        if (disc == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[获取碟片失败][指定的碟片Asin不存在][Asin={}]", asin);
            }
            return errorMessage("指定的碟片Asin不存在");
        }

        return responseDisc(disc);
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @PutMapping(value = "/api/discs2/{id}", produces = MEDIA_TYPE)
    public String setOne2(@PathVariable Long id,
                         @JsonArg String titlePc,
                         @JsonArg DiscType discType,
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
        disc.setDiscType(discType);
        disc.setReleaseDate(localDate);

        JSONObject result = disc.toJSON();

        if (LOGGER.isDebugEnabled()) {
            debugRequest("[编辑碟片成功][修改后={}]", result);
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
        return errorMessage("不支持的操作");
    }

    @Transactional
    @PutMapping(value = "/api/discs/{id}/pts", produces = MEDIA_TYPE)
    public String mergePts(@PathVariable Long id, @JsonArg String text) {
        return errorMessage("不支持的操作");
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @GetMapping(value = "/api/discs/search/{asin}", produces = MEDIA_TYPE)
    public String search(@PathVariable String asin) {
        LOGGER.info("申请查询碟片, ASIN={}", asin);
        Disc disc = dao.lookup(Disc.class, "asin", asin);
        if (disc == null) {
            LOGGER.info("开始从日亚查询碟片, ASIN={}", asin);
            JSONObject root = amazonDiscSpider.fetchDiscInfo(asin);
            if (root.getBoolean("success")) {
                JSONObject discInfo = root.getJSONObject("data");
                disc = new Disc(
                        asin,
                        discInfo.getString("title"),
                        DiscType.valueOf(discInfo.getString("type")),
                        UpdateType.Both,
                        false,
                        LocalDate.parse(discInfo.getString("date"), formatter2)
                );
                dao.save(disc);
            } else {
                return root.toString();
            }
        }

        JSONArray result = new JSONArray();
        JSONObject discJSON = disc.toJSON();

        amazonNewDiscSpider.updateNewDiscFollowd(disc);

        if (LOGGER.isInfoEnabled()) {
            infoRequest("[查找碟片成功][碟片信息={}]", discJSON);
        }
        result.put(discJSON);
        return objectResult(result);
    }

}
