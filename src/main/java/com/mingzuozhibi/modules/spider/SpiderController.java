package com.mingzuozhibi.modules.spider;

import com.mingzuozhibi.commons.base.BaseKeys.Name;
import com.mingzuozhibi.commons.base.PageController;
import com.mingzuozhibi.commons.logger.LoggerBind;
import com.mingzuozhibi.modules.disc.DiscRepository;
import com.mingzuozhibi.modules.record.RecordCompute;
import com.mingzuozhibi.modules.vultr.TaskOfContent;
import com.mingzuozhibi.modules.vultr.VultrContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.mingzuozhibi.commons.base.BaseKeys.FETCH_TASK_START;
import static com.mingzuozhibi.support.ChecksUtils.paramNotExists;
import static com.mingzuozhibi.support.ModifyUtils.*;

@RestController
@LoggerBind(Name.SERVER_USER)
public class SpiderController extends PageController {

    @Autowired
    private VultrContext vultrContext;

    @Autowired
    private RecordCompute recordCompute;

    @Autowired
    private ContentService contentService;

    @Autowired
    private DiscRepository discRepository;

    @Autowired
    private HistoryRepository historyRepository;

    @Transactional
    @GetMapping(value = "/api/spider/discShelfs", produces = MEDIA_TYPE)
    public String discShelfs(@RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "20") int size) {
        if (size > 40) {
            return errorResult("Size不能大于40");
        }
        var pageRequest = PageRequest.of(page - 1, size, Sort.by(Order.desc("id")));
        var pageResult = historyRepository.findAll(pageRequest);
        return pageResult(pageResult);
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @GetMapping(value = "/api/spider/fetchCount", produces = MEDIA_TYPE)
    public String fetchCount() {
        return dataResult(discRepository.countActiveDiscs());
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @GetMapping(value = "/api/spider/searchDisc/{asin}", produces = MEDIA_TYPE)
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

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @PostMapping(value = "/api/spider/computePt/{id}", produces = MEDIA_TYPE)
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

    @Transactional
    @GetMapping(value = "/admin/setDisable/{disable}")
    public void setDisable(@PathVariable("disable") Boolean next) {
        var bean = vultrContext.getDisable();
        var prev = bean.getValue();
        bean.setValue(next);
        if (!Objects.equals(prev, next)) amqpSender.bind(Name.SERVER_CORE)
            .notify("Change Vultr Disable = %b".formatted(next));
    }

    @Transactional
    @GetMapping(value = "/admin/sendTasks")
    public void sendTasks() {
        var tasks = discRepository.findNeedUpdate().stream()
            .map(disc -> new TaskOfContent(disc.getAsin(), disc.getThisRank()))
            .collect(Collectors.toList());
        amqpSender.send(FETCH_TASK_START, gson.toJson(tasks));
        bind.info("JMS -> %s size=%d".formatted(FETCH_TASK_START, tasks.size()));
    }

}
