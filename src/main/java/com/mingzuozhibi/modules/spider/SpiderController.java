package com.mingzuozhibi.modules.spider;

import com.mingzuozhibi.commons.base.BaseController;
import com.mingzuozhibi.commons.domain.Result;
import com.mingzuozhibi.modules.disc.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;

import static com.mingzuozhibi.utils.ModifyUtils.logCreate;

@RestController
public class SpiderController extends BaseController {

    @Autowired
    private DiscUpdateApi discUpdateApi;

    @Autowired
    private DiscRepository discRepository;

    @Autowired
    private GroupService groupService;

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @GetMapping(value = "/api/admin/fetchCount")
    public String getFetchCount() {
        return dataResult(groupService.findNeedUpdateAsins().size());
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @GetMapping(value = "/api/admin/searchDisc/{asin}", produces = MEDIA_TYPE)
    public String searchDisc(@PathVariable String asin) {
        Optional<Disc> byAsin = discRepository.findByAsin(asin);
        if (byAsin.isPresent()) {
            // 碟片已存在
            return dataResult(byAsin.get().toJson());
        }
        Result<DiscUpdate> result = discUpdateApi.doGet(asin);
        if (result.hasError()) {
            // 查询失败
            return errorResult(result.getMessage());
        }
        Disc disc = discUpdateApi.createWith(result.getData());
        if (disc.getReleaseDate() == null) {
            // 检查日期
            jmsMessage.warning("创建碟片时未发现发售日期，碟片=%s", disc.getLogName());
            disc.setReleaseDate(LocalDate.now());
        }
        // 创建碟片
        discRepository.save(disc);
        jmsMessage.success(logCreate("碟片", disc.getTitle(), disc.getLogName()));
        return dataResult(disc);
    }

}
