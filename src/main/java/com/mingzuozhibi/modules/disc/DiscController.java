package com.mingzuozhibi.modules.disc;

import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.base.BaseController;
import com.mingzuozhibi.commons.mylog.JmsEnums.Name;
import com.mingzuozhibi.commons.mylog.JmsLogger;
import com.mingzuozhibi.modules.disc.Disc.DiscType;
import com.mingzuozhibi.modules.record.RecordService;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import static com.mingzuozhibi.commons.utils.FormatUtils.fmtDate;
import static com.mingzuozhibi.modules.spider.SpiderUpdater.updateRank;
import static com.mingzuozhibi.utils.ChecksUtils.*;
import static com.mingzuozhibi.utils.ModifyUtils.*;

@RestController
public class DiscController extends BaseController {

    private JmsLogger bind;

    @PostConstruct
    public void bind() {
        bind = jmsSender.bind(Name.SERVER_USER);
    }

    @Autowired
    private RecordService recordService;

    @Autowired
    private DiscRepository discRepository;

    @Transactional
    @GetMapping(value = "/api/discs/{id}", produces = MEDIA_TYPE)
    public String findById(@PathVariable Long id) {
        Optional<Disc> byId = discRepository.findById(id);
        if (!byId.isPresent()) {
            return paramNotExists("碟片ID");
        }
        return dataResult(byId.get().toJson());
    }

    @Transactional
    @GetMapping(value = "/api/discs/asin/{asin}", produces = MEDIA_TYPE)
    public String findByAsin(@PathVariable String asin) {
        Optional<Disc> byId = discRepository.findByAsin(asin);
        if (!byId.isPresent()) {
            return paramNotExists("碟片ASIN");
        }
        return dataResult(byId.get().toJson());
    }

    @Transactional
    @GetMapping(value = "/api/discs/{id}/records", produces = MEDIA_TYPE)
    public String findRecords(@PathVariable Long id) {
        Optional<Disc> byId = discRepository.findById(id);
        if (!byId.isPresent()) {
            return paramNotExists("碟片ID");
        }
        Disc disc = byId.get();
        JsonObject object = gson.toJsonTree(disc).getAsJsonObject();
        object.add("records", recordService.buildRecords(disc));
        return dataResult(object);
    }

    @Setter
    private static class CreateForm {
        private String asin;
        private String title;
        private DiscType discType;
        private String releaseDate;
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @PostMapping(value = "/api/discs", produces = MEDIA_TYPE)
    public String doCreate(@RequestBody CreateForm form) {
        Optional<String> checks = runChecks(
            checkNotEmpty(form.asin, "ASIN"),
            checkStrMatch(form.asin, "ASIN", "[A-Z0-9]{10}"),
            checkNotEmpty(form.discType, "碟片类型"),
            checkNotEmpty(form.releaseDate, "发售日期"),
            checkStrMatch(form.releaseDate, "发售日期", "\\d{4}/\\d{1,2}/\\d{1,2}")
        );
        if (checks.isPresent()) {
            return errorResult(checks.get());
        }
        if (StringUtils.isEmpty(form.title)) {
            form.title = form.asin;
        }
        if (discRepository.existsByAsin(form.asin)) {
            return paramExists("ASIN");
        }
        LocalDate localDate = LocalDate.parse(form.releaseDate, fmtDate);
        Disc disc = new Disc(form.asin, form.title, form.discType, localDate);
        discRepository.save(disc);
        bind.success(logCreate("碟片", disc.getLogName(), gson.toJson(disc)));
        return dataResult(disc.toJson());
    }

    @Setter
    private static class UpdateForm {
        private String titlePc;
        private DiscType discType;
        private String releaseDate;
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @PutMapping(value = "/api/discs/{id}", produces = MEDIA_TYPE)
    public String doUpdate(@PathVariable Long id,
                           @RequestBody UpdateForm form) {
        Optional<String> checks = runChecks(
            checkNotEmpty(form.discType, "碟片类型"),
            checkNotEmpty(form.releaseDate, "发售日期"),
            checkStrMatch(form.releaseDate, "发售日期", "\\d{4}/\\d{1,2}/\\d{1,2}")
        );
        if (checks.isPresent()) {
            return errorResult(checks.get());
        }
        LocalDate localDate = LocalDate.parse(form.releaseDate, fmtDate);
        Optional<Disc> byId = discRepository.findById(id);
        if (!byId.isPresent()) {
            return paramNotExists("碟片ID");
        }
        Disc disc = byId.get();
        if (!Objects.equals(disc.getTitlePc(), form.titlePc)) {
            bind.info(logUpdate("碟片标题", disc.getTitlePc(), form.titlePc));
            disc.setTitlePc(form.titlePc);
        }
        if (!Objects.equals(disc.getDiscType(), form.discType)) {
            bind.notify(logUpdate("碟片类型", disc.getDiscType(), form.discType));
            disc.setDiscType(form.discType);
        }
        if (!Objects.equals(disc.getReleaseDate(), localDate)) {
            bind.notify(logUpdate("发售日期", disc.getReleaseDate(), localDate));
            disc.setReleaseDate(localDate);
        }
        return dataResult(disc.toJson());
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @PostMapping(value = "/api/updateRank/{asin}/{rank}", produces = MEDIA_TYPE)
    public String doUpdateRank(@PathVariable("asin") String asin,
                               @PathVariable("rank") Integer rank) {
        Optional<Disc> byAsin = discRepository.findByAsin(asin);
        if (!byAsin.isPresent()) {
            return paramNotExists("碟片ASIN");
        }
        Disc disc = byAsin.get();
        updateRank(disc, rank, Instant.now());
        bind.debug(logUpdate("碟片排名", disc.getPrevRank(), disc.getThisRank(), disc.getLogName()));
        return dataResult(disc.toJson());
    }

}
