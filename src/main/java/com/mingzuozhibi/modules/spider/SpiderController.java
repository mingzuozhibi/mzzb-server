package com.mingzuozhibi.modules.spider;

import com.mingzuozhibi.commons.base.PageController;
import com.mingzuozhibi.commons.domain.Result;
import com.mingzuozhibi.commons.mylog.JmsBind;
import com.mingzuozhibi.commons.mylog.JmsEnums.Name;
import com.mingzuozhibi.modules.disc.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.domain.Sort.Order;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static com.mingzuozhibi.support.ModifyUtils.logCreate;

@RestController
@JmsBind(Name.SERVER_USER)
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
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Order.desc("id")));
        Page<History> pageResult = historyRepository.findAll(pageRequest);
        return pageResult(pageResult);
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @GetMapping(value = "/api/spider/fetchCount", produces = MEDIA_TYPE)
    public String getFetchCount() {
        Set<String> needUpdateAsins = groupService.findNeedUpdateAsins();
        return dataResult(needUpdateAsins.size());
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @GetMapping(value = "/api/spider/searchDisc/{asin}", produces = MEDIA_TYPE)
    public String searchDisc(@PathVariable String asin) {
        Optional<Disc> byAsin = discRepository.findByAsin(asin);
        if (byAsin.isPresent()) {
            // 碟片已存在
            return dataResult(byAsin.get().toJson());
        }
        Result<Content> result = contentService.doGet(asin);
        if (result.hasError()) {
            // 查询失败
            return errorResult(result.getMessage());
        }
        Disc disc = contentService.createWith(result.getData());
        if (disc.getReleaseDate() == null) {
            // 检查日期
            bind.warning("[创建碟片时缺少发售日期][碟片=%s]", disc.getLogName());
            disc.setReleaseDate(LocalDate.now());
        }
        // 创建碟片
        discRepository.save(disc);
        historyRepository.setTracked(asin, true);
        bind.success(logCreate("碟片(查询)", disc.getLogName(), disc.toJson().toString()));
        return dataResult(disc.toJson());
    }

}
