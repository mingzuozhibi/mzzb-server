package mingzuozhibi.action;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.Disc.DiscType;
import mingzuozhibi.support.JsonArg;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import static mingzuozhibi.utils.RecordUtils.buildRecords;

@RestController
public class DiscController extends BaseController {

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
    @PreAuthorize("hasRole('Disc_Admin')")
    @PutMapping(value = "/api/discs/{id}", produces = MEDIA_TYPE)
    public String setOne(@PathVariable Long id,
                         @JsonArg String titlePc,
                         @JsonArg DiscType discType,
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

        // 修改中
        disc.setTitlePc(titlePc);
        disc.setDiscType(discType);
        disc.setReleaseDate(localDate);

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
