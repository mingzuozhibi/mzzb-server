package com.mingzuozhibi.modules.disc;

import com.mingzuozhibi.commons.base.BaseKeys.Name;
import com.mingzuozhibi.commons.base.PageController;
import com.mingzuozhibi.commons.logger.LoggerBind;
import com.mingzuozhibi.modules.disc.Disc.DiscType;
import com.mingzuozhibi.modules.record.RecordService;
import com.mingzuozhibi.modules.spider.HistoryRepository;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static com.mingzuozhibi.commons.base.BaseController.DEFAULT_TYPE;
import static com.mingzuozhibi.commons.utils.MyTimeUtils.fmtDate;
import static com.mingzuozhibi.modules.disc.DiscUtils.updateRank;
import static com.mingzuozhibi.support.ChecksUtils.*;
import static com.mingzuozhibi.support.ModifyUtils.*;

@LoggerBind(Name.SERVER_USER)
@Transactional
@RestController
@RequestMapping(produces = DEFAULT_TYPE)
public class DiscController extends PageController {

    @Autowired
    private RecordService recordService;

    @Autowired
    private DiscRepository discRepository;

    @Autowired
    private HistoryRepository historyRepository;

    @GetMapping("/api/discs")
    public String findAll(@RequestParam(required = false) String title, Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            return errorResult("Size不能大于100");
        }
        var spec = (Specification<Disc>) (root, query, cb) -> {
            List<Predicate> predicates = new LinkedList<>();
            if (!StringUtils.isBlank(title)) {
                Arrays.stream(title.trim().split("\\s+")).forEach(text -> {
                    predicates.add(cb.or(
                        cb.like(root.get("title"), "%" + text.trim() + "%"),
                        cb.like(root.get("titlePc"), "%" + text.trim() + "%")
                    ));
                });
            }
            return query.where(predicates.toArray(Predicate[]::new)).getRestriction();
        };
        var sort = Sort.sort(Disc.class).by(Disc::getId).descending();
        return pageResult(discRepository.findAll(spec, pageRequest(pageable, sort)).map(Disc::toJson));
    }

    @GetMapping("/api/discs/{id}")
    public String findById(@PathVariable Long id) {
        var byId = discRepository.findById(id);
        if (byId.isEmpty()) {
            return paramNotExists("碟片ID");
        }
        return dataResult(byId.get().toJson());
    }

    @GetMapping("/api/discs/asin/{asin}")
    public String findByAsin(@PathVariable String asin) {
        var byAsin = discRepository.findByAsin(asin);
        if (byAsin.isEmpty()) {
            return paramNotExists("碟片ASIN");
        }
        return dataResult(byAsin.get().toJson());
    }

    @GetMapping("/api/discs/{id}/records")
    public String findRecords(@PathVariable Long id) {
        var byId = discRepository.findById(id);
        if (byId.isEmpty()) {
            return paramNotExists("碟片ID");
        }
        var disc = byId.get();
        var object = gson.toJsonTree(disc).getAsJsonObject();
        object.add("records", recordService.buildDiscRecords(disc));
        return dataResult(object);
    }

    @Setter
    private static class CreateForm {
        private String asin;
        private String title;
        private DiscType discType;
        private String releaseDate;
    }

    @PreAuthorize("hasRole('BASIC')")
    @PostMapping("/api/discs")
    public String doCreate(@RequestBody CreateForm form) {
        var checks = runChecks(
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
        var localDate = LocalDate.parse(form.releaseDate, fmtDate);
        var disc = new Disc(form.asin, form.title, form.discType, localDate);
        discRepository.save(disc);
        historyRepository.setTracked(form.asin, true);
        bind.success(logCreate("碟片(手动)", disc.getLogName(), disc.toJson().toString()));
        return dataResult(disc.toJson());
    }

    @Setter
    private static class UpdateForm {
        private String titlePc;
        private DiscType discType;
        private String releaseDate;
    }

    @PreAuthorize("hasRole('BASIC')")
    @PutMapping("/api/discs/{id}")
    public String doUpdate(@PathVariable Long id,
                           @RequestBody UpdateForm form) {
        var checks = runChecks(
            checkNotEmpty(form.discType, "碟片类型"),
            checkNotEmpty(form.releaseDate, "发售日期"),
            checkStrMatch(form.releaseDate, "发售日期", "\\d{4}/\\d{1,2}/\\d{1,2}")
        );
        if (checks.isPresent()) {
            return errorResult(checks.get());
        }
        var localDate = LocalDate.parse(form.releaseDate, fmtDate);
        var byId = discRepository.findById(id);
        if (byId.isEmpty()) {
            return paramNotExists("碟片ID");
        }
        var disc = byId.get();
        if (!Objects.equals(disc.getTitlePc(), form.titlePc)) {
            bind.info(logUpdate("碟片标题", disc.getTitlePc(), form.titlePc, disc.getLogName()));
            disc.setTitlePc(form.titlePc);
        }
        if (!Objects.equals(disc.getDiscType(), form.discType)) {
            bind.notify(logUpdate("碟片类型", disc.getDiscType(), form.discType, disc.getLogName()));
            disc.setDiscType(form.discType);
        }
        if (!Objects.equals(disc.getReleaseDate(), localDate)) {
            bind.notify(logUpdate("发售日期", disc.getReleaseDate(), localDate, disc.getLogName()));
            disc.setReleaseDate(localDate);
        }
        return dataResult(disc.toJson());
    }

    @Setter
    private static class PatchForm {
        Integer rank;
    }

    @PreAuthorize("hasRole('BASIC')")
    @PatchMapping("/api/discs/{id}")
    public String doPatch(@PathVariable Long id, @RequestBody PatchForm form) {
        var byId = discRepository.findById(id);
        if (byId.isEmpty()) {
            return paramNotExists("碟片ID");
        }
        var disc = byId.get();
        if (form.rank != null && !(Objects.equals(form.rank, disc.getThisRank()))) {
            updateRank(disc, form.rank, Instant.now());
            amqpSender.bind(Name.DEFAULT).debug(logUpdate("碟片排名",
                disc.getPrevRank(), disc.getThisRank(), disc.getLogName()));
        }
        return dataResult(disc.toJson());
    }

}
