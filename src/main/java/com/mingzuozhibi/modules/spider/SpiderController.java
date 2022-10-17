package com.mingzuozhibi.modules.spider;

import com.mingzuozhibi.commons.base.BaseEntity;
import com.mingzuozhibi.commons.base.BaseKeys.Name;
import com.mingzuozhibi.commons.base.PageController;
import com.mingzuozhibi.commons.logger.LoggerBind;
import com.mingzuozhibi.modules.disc.DiscRepository;
import com.mingzuozhibi.modules.record.RecordCompute;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.*;

import static com.mingzuozhibi.commons.base.BaseController.DEFAULT_TYPE;
import static com.mingzuozhibi.support.ChecksUtils.paramNotExists;
import static com.mingzuozhibi.support.ModifyUtils.*;

@LoggerBind(Name.SERVER_USER)
@Transactional
@RestController
@RequestMapping(produces = DEFAULT_TYPE)
public class SpiderController extends PageController {

    @Autowired
    private RecordCompute recordCompute;

    @Autowired
    private ContentService contentService;

    @Autowired
    private DiscRepository discRepository;

    @Autowired
    private HistoryRepository historyRepository;

    @GetMapping("/api/spider/historys")
    public String findAll(@RequestParam(required = false) String asin,
                          @RequestParam(required = false) String type,
                          @RequestParam(required = false) String title,
                          @RequestParam(required = false) Boolean tracked,
                          Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            return errorResult("Size不能大于100");
        }
        var spec = (Specification<History>) (root, query, cb) -> {
            List<Predicate> predicates = new LinkedList<>();
            if (!StringUtils.isBlank(asin)) {
                predicates.add(cb.equal(root.get("asin"), asin));
            }
            if (!StringUtils.isBlank(type)) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (!StringUtils.isBlank(title)) {
                Arrays.stream(title.trim().split("\\s+")).forEach(text -> {
                    predicates.add(cb.like(root.get("title"), "%" + text + "%"));
                });
            }
            if (!Objects.isNull(tracked)) {
                predicates.add(cb.equal(root.get("tracked"), tracked));
            }
            return query.where(predicates.toArray(Predicate[]::new)).getRestriction();
        };
        var sort = Sort.sort(History.class).by(BaseEntity::getId).descending();
        return pageResult(historyRepository.findAll(spec, pageRequest(pageable, sort)));
    }

    @PreAuthorize("hasRole('BASIC')")
    @GetMapping("/api/spider/fetchCount")
    public String fetchCount() {
        return dataResult(discRepository.countActiveDiscs());
    }

    @PreAuthorize("hasRole('BASIC')")
    @GetMapping("/api/spider/searchDisc/{asin}")
    public String searchDisc(@PathVariable String asin) {
        var byAsin = discRepository.findByAsin(asin);
        if (byAsin.isPresent()) {
            // 碟片已存在
            return dataResult(byAsin.get().toJson());
        }
        var result = contentService.doGet(asin);
        if (result.hasError()) {
            // 查询失败
            return errorResult(result.getMessage());
        }
        var disc = contentService.createWith(result.getData());
        if (disc.getReleaseDate() == null) {
            // 检查日期
            bind.warning("[创建碟片时缺少发售日期][碟片=%s]".formatted(disc.getLogName()));
            disc.setReleaseDate(LocalDate.now());
        }
        // 创建碟片
        discRepository.save(disc);
        historyRepository.setTracked(asin, true);
        bind.success(logCreate("碟片(查询)", disc.getLogName(), disc.toJson().toString()));
        return dataResult(disc.toJson());
    }

    @PreAuthorize("hasRole('BASIC')")
    @PostMapping("/api/spider/computePt/{id}")
    public String computePt(@PathVariable Long id) {
        var byId = discRepository.findById(id);
        if (byId.isEmpty()) {
            return paramNotExists("碟片ID");
        }
        var disc = byId.get();
        var pt1 = disc.getTotalPt();
        recordCompute.computeDisc(disc);
        var pt2 = disc.getTotalPt();
        bind.info(logUpdate("碟片PT", pt1, pt2, disc.getLogName()));
        return dataResult("compute: " + pt1 + "->" + pt2);
    }

}
