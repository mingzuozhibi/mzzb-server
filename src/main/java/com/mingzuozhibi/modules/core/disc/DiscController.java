package com.mingzuozhibi.modules.core.disc;

import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.base.BaseController2;
import com.mingzuozhibi.modules.core.disc.Disc.DiscType;
import com.mingzuozhibi.modules.core.record.DiscRecordService;
import com.mingzuozhibi.support.JsonArg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

import static com.mingzuozhibi.commons.utils.ChecksUtils.*;
import static com.mingzuozhibi.commons.utils.ModifyUtils.logCreate;
import static com.mingzuozhibi.commons.utils.ModifyUtils.logUpdate;

@RestController
public class DiscController extends BaseController2 {

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy/M/d");

    @Autowired
    private DiscRepository discRepository;

    @Autowired
    private DiscRecordService discRecordService;

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
    @PreAuthorize("hasRole('BASIC')")
    @PostMapping(value = "/api/discs", produces = MEDIA_TYPE)
    public String doCreate(@JsonArg String asin,
                           @JsonArg String title,
                           @JsonArg DiscType discType,
                           @JsonArg String releaseDate) {
        Optional<String> checks = runChecks(
            checkSelected(discType, "碟片类型"),
            checkNotEmpty(releaseDate, "发售日期"),
            checkDateText(releaseDate, "发售日期", "yyyy/M/d")
        );
        if (checks.isPresent()) {
            return errorResult(checks.get());
        }
        LocalDate localDate = LocalDate.parse(releaseDate, FORMATTER);
        Disc disc = new Disc(asin, title, discType, localDate);
        discRepository.save(disc);
        jmsMessage.success(logCreate("碟片", disc.getLogName(), gson.toJson(disc)));
        return dataResult(disc.toJson());
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @PutMapping(value = "/api/discs/{id}", produces = MEDIA_TYPE)
    public String doUpdate(@PathVariable Long id,
                           @JsonArg String titlePc,
                           @JsonArg DiscType discType,
                           @JsonArg String releaseDate) {
        Optional<String> checks = runChecks(
            checkSelected(discType, "碟片类型"),
            checkNotEmpty(releaseDate, "发售日期"),
            checkDateText(releaseDate, "发售日期", "yyyy/M/d")
        );
        if (checks.isPresent()) {
            return errorResult(checks.get());
        }
        LocalDate localDate = LocalDate.parse(releaseDate, FORMATTER);
        Optional<Disc> byId = discRepository.findById(id);
        if (!byId.isPresent()) {
            return paramNotExists("碟片ID");
        }
        Disc disc = byId.get();
        if (!Objects.equals(disc.getTitlePc(), titlePc)) {
            disc.setTitlePc(titlePc);
            jmsMessage.info(logUpdate("碟片标题", disc.getTitlePc(), titlePc));
        }
        if (!Objects.equals(disc.getDiscType(), discType)) {
            disc.setDiscType(discType);
            jmsMessage.info(logUpdate("碟片类型", disc.getDiscType(), discType));
        }
        if (!Objects.equals(disc.getReleaseDate(), localDate)) {
            disc.setReleaseDate(localDate);
            jmsMessage.info(logUpdate("发售日期", disc.getReleaseDate(), localDate));
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
        disc.setPrevRank(disc.getThisRank());
        disc.setThisRank(rank);
        LocalDateTime now = LocalDateTime.now();
        disc.setModifyTime(now);
        disc.setUpdateTime(now);
        jmsMessage.info(logUpdate("碟片排名", disc.getPrevRank(), disc.getThisRank(), disc.getLogName()));
        return dataResult(disc.toJson());
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
        object.add("records", discRecordService.findRecords(disc));
        return dataResult(object);
    }

}
