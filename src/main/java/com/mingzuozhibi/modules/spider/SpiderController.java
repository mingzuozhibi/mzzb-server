package com.mingzuozhibi.modules.spider;

import com.mingzuozhibi.commons.base.BaseKeys.Name;
import com.mingzuozhibi.commons.base.PageController;
import com.mingzuozhibi.commons.logger.LoggerBind;
import com.mingzuozhibi.modules.disc.DiscRepository;
import com.mingzuozhibi.modules.disc.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import static com.mingzuozhibi.support.ModifyUtils.logCreate;

@RestController
@LoggerBind(Name.SERVER_USER)
public class SpiderController extends PageController {

    @Autowired
    private GroupService groupService;

    @Autowired
    private ContentService contentService;

    @Autowired
    private DiscRepository discRepository;

    @Autowired
    private HistoryRepository historyRepository;

    @Transactional
    @GetMapping(value = "/api/spider/discShelfs", produces = MEDIA_TYPE)
    public String findHistory(@RequestParam(defaultValue = "1") int page,
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
    public String getFetchCount() {
        return dataResult(groupService.getFetchCount());
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

}
