package com.mingzuozhibi.modules.spider;

import com.mingzuozhibi.commons.base.PageController;
import com.mingzuozhibi.commons.domain.Result;
import com.mingzuozhibi.commons.mylog.JmsEnums.Name;
import com.mingzuozhibi.commons.mylog.JmsLogger;
import com.mingzuozhibi.modules.disc.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.Optional;

import static com.mingzuozhibi.support.ModifyUtils.logCreate;

@RestController
public class SpiderController extends PageController {

    private JmsLogger bind;

    @PostConstruct
    public void bind() {
        bind = jmsSender.bind(Name.SERVER_USER);
    }

    @Autowired
    private ContentApi contentApi;

    @Autowired
    private GroupService groupService;

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
        return pageResult(historyRepository.findAll(pageRequest));
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @GetMapping(value = "/api/spider/fetchCount", produces = MEDIA_TYPE)
    public String getFetchCount() {
        return dataResult(groupService.findNeedUpdateAsins().size());
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
        Result<Content> result = contentApi.doGet(asin);
        if (result.hasError()) {
            // 查询失败
            return errorResult(result.getMessage());
        }
        Disc disc = contentApi.createWith(result.getData());
        if (disc.getReleaseDate() == null) {
            // 检查日期
            bind.warning("创建碟片时缺少发售日期, 碟片=%s", disc.getLogName());
            disc.setReleaseDate(LocalDate.now());
        }
        // 创建碟片
        discRepository.save(disc);
        historyRepository.setTracked(asin, true);
        bind.success(logCreate("碟片", disc.getTitle(), disc.getLogName()));
        return dataResult(disc.toJson());
    }

}
