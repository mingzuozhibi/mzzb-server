package mingzuozhibi.action;

import mingzuozhibi.jms.JmsMessage;
import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.Disc.DiscType;
import mingzuozhibi.support.JsonArg;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import static mingzuozhibi.utils.RecordUtils.buildRecords;

@RestController
public class DiscController extends BaseController {

    @Autowired
    private JmsMessage jmsMessage;

    @Transactional
    @PostMapping(value = "/api/updateRank/{asin}/{rank}", produces = MEDIA_TYPE)
    public String updateRank(@PathVariable("asin") String asin,
                             @PathVariable("rank") Integer rank) {
        Disc disc = dao.lookup(Disc.class, "asin", asin);
        if (disc == null) {
            return errorMessage("指定的碟片Asin不存在");
        }
        disc.setPrevRank(disc.getThisRank());
        disc.setThisRank(rank);
        LocalDateTime now = LocalDateTime.now();
        disc.setModifyTime(now);
        disc.setUpdateTime(now);
        jmsMessage.info("%s 更新了[%s]的排名: %d->%d, 标题: %s",
            getUserName(), asin, disc.getPrevRank(), disc.getThisRank(), disc.getTitle());
        return objectResult(disc.toJSON());
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

        return objectResult(disc.toJSON());
    }

    @Transactional
    @GetMapping(value = "/api/discs/asin/{asin}", produces = MEDIA_TYPE)
    public String findByAsin(@PathVariable String asin) {
        Disc disc = dao.lookup(Disc.class, "asin", asin);

        if (disc == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[获取碟片失败][指定的碟片Asin不存在][Asin={}]", asin);
            }
            return errorMessage("指定的碟片Asin不存在");
        }

        return objectResult(disc.toJSON());
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @PostMapping(value = "/api/discs", produces = MEDIA_TYPE)
    public String addOne(@JsonArg String asin, @JsonArg String title, @JsonArg DiscType discType,
                         @JsonArg String releaseDate) {
        // 校验
        ReleaseDateChecker dateChecker = new ReleaseDateChecker(releaseDate);
        if (dateChecker.hasError()) {
            return errorMessage(dateChecker.error);
        }
        LocalDate localDate = dateChecker.getData();

        // 创建
        Disc disc = new Disc(asin, title, discType, localDate);
        dao.save(disc);

        JSONObject result = disc.toJSON();
        jmsMessage.info("%s 创建碟片[%s], disc=%s", getUserName(), asin, result.toString());
        return objectResult(result);
    }


    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @PutMapping(value = "/api/discs/{id}", produces = MEDIA_TYPE)
    public String setOne(@PathVariable Long id, @JsonArg String titlePc, @JsonArg DiscType discType,
                         @JsonArg String releaseDate) {
        // 校验
        ReleaseDateChecker dateChecker = new ReleaseDateChecker(releaseDate);
        if (dateChecker.hasError()) {
            return errorMessage(dateChecker.error);
        }
        LocalDate localDate = dateChecker.getData();

        // 查询
        Disc disc = dao.get(Disc.class, id);
        if (disc == null) {
            if (LOGGER.isWarnEnabled()) {
                warnRequest("[编辑碟片失败][指定的碟片Id不存在][Id={}]", id);
            }
            return errorMessage("指定的碟片Id不存在");
        }

        // 修改前
        JSONObject before = disc.toJSON();
        if (LOGGER.isInfoEnabled()) {
            infoRequest("[编辑碟片开始][修改前={}]", before);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        // 修改中
        if (!Objects.equals(disc.getTitlePc(), titlePc)) {
            jmsMessage.info("[用户=%s][修改碟片标题][%s][%s=>%s]", getUserName(), disc.getAsin(), disc.getTitlePc(), titlePc);
            disc.setTitlePc(titlePc);
        }
        if (!Objects.equals(disc.getDiscType(), discType)) {
            jmsMessage.info("[用户=%s][修改碟片类型][%s][%s=>%s]", getUserName(), disc.getAsin(), disc.getDiscType().name(),
                discType.name());
            disc.setDiscType(discType);
        }
        if (!Objects.equals(disc.getReleaseDate(), localDate)) {
            jmsMessage.info("[用户=%s][修改碟片发售日期][%s][%s=>%s]", getUserName(), disc.getAsin(),
                disc.getReleaseDate().format(formatter), localDate.format(formatter));
            disc.setReleaseDate(localDate);
        }

        // 修改后
        JSONObject result = disc.toJSON();
        if (LOGGER.isInfoEnabled()) {
            infoRequest("[编辑碟片成功][修改后={}]", result);
        }
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
        result.put("records", buildRecords(dao, disc));
        return objectResult(result);
    }

    public class ReleaseDateChecker {
        private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        private final String input;
        private LocalDate data;
        private String error;

        public ReleaseDateChecker(String input) {
            this.input = input;
        }

        public boolean hasError() {
            if (StringUtils.isEmpty(input)) {
                if (LOGGER.isWarnEnabled()) {
                    warnRequest("[编辑碟片失败][发售日期不能为空]");
                }
                return setError("发售日期不能为空");
            }
            try {
                return setData(LocalDate.parse(input, formatter));
            } catch (DateTimeParseException e) {
                if (LOGGER.isWarnEnabled()) {
                    warnRequest("[编辑碟片失败][发售日期格式不正确]");
                }
                return setError("发售日期格式不正确");
            }
        }

        private boolean setData(LocalDate data) {
            this.data = data;
            return false;
        }

        public LocalDate getData() {
            return data;
        }

        private boolean setError(String error) {
            this.error = error;
            return true;
        }

        public String getError() {
            return error;
        }
    }

}
