package com.mingzuozhibi.modules.spider;

import com.google.gson.reflect.TypeToken;
import com.mingzuozhibi.commons.base.BaseController2;
import com.mingzuozhibi.commons.mylog.JmsService;
import com.mingzuozhibi.modules.disc.Disc;
import com.mingzuozhibi.modules.disc.DiscRepository;
import com.mingzuozhibi.modules.group.DiscGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.mingzuozhibi.commons.utils.FormatUtils.DATE_FORMATTER;
import static com.mingzuozhibi.commons.utils.ModifyUtils.logCreate;

@RestController
public class DiscSpiderController extends BaseController2 {

    @Autowired
    private JmsService jmsService;

    @Autowired
    private DiscSpider discSpider;

    @Autowired
    private DiscUpdater discUpdater;

    @Autowired
    private DiscGroupService discGroupService;

    @Autowired
    private DiscRepository discRepository;

    @Scheduled(cron = "0 59 * * * ?")
    @GetMapping("/admin/sendNeedUpdateAsins")
    public void sendNeedUpdateAsins() {
        Set<String> asins = discGroupService.findNeedUpdateAsins();
        jmsService.convertAndSend("need.update.asins", gson.toJson(asins));
        jmsMessage.notify("JMS -> need.update.asins size=" + asins.size());
    }

    @JmsListener(destination = "prev.update.discs")
    public void discSpiderUpdate(String json) {
        TypeToken<?> typeToken = TypeToken.getParameterized(ArrayList.class, DiscInfo.class);
        List<DiscInfo> discInfos = gson.fromJson(json, typeToken.getType());
        jmsMessage.notify("JMS <- prev.update.discs size=" + discInfos.size());
        discUpdater.updateDiscs(discInfos);
    }

    @JmsListener(destination = "done.update.discs")
    public void discSpiderUpdate2(String json) {
        TypeToken<?> typeToken = TypeToken.getParameterized(ArrayList.class, DiscInfo.class);
        List<DiscInfo> discInfos = gson.fromJson(json, typeToken.getType());
        jmsMessage.notify("JMS <- done.update.discs size=" + discInfos.size());
        discUpdater.updateDiscs(discInfos);
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @GetMapping(value = "/api/admin/fetchCount")
    public String getFetchCount() {
        return dataResult(discGroupService.findNeedUpdateAsins().size());
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @GetMapping(value = "/api/admin/searchDisc/{asin}", produces = MEDIA_TYPE)
    public String searchDisc(@PathVariable String asin) {
        Optional<Disc> byAsin = discRepository.findByAsin(asin);
        if (byAsin.isPresent()) {
            return dataResult(byAsin.get().toJson());
        }
        SearchTask<DiscInfo> task = discSpider.sendDiscUpdate(asin);
        if (!task.isSuccess()) {
            return errorResult(task.getMessage());
        }
        DiscInfo discInfo = task.getData();
        if (discInfo.isOffTheShelf()) {
            return errorResult("可能该碟片已下架");
        }
        Disc disc = createDisc(asin, discInfo);
        jmsService.sendDiscTrack(disc.getAsin(), disc.getTitle());
        jmsMessage.success(logCreate("碟片", disc.getTitle(), disc.getLogName()));
        return dataResult(disc);
    }


    private Disc createDisc(@PathVariable String asin, DiscInfo discInfo) {
        String title = discInfo.getTitle();
        Disc.DiscType discType = Disc.DiscType.valueOf(discInfo.getType());
        LocalDate releaseDate = Optional.ofNullable(discInfo.getDate())
            .map(date -> LocalDate.parse(date, DATE_FORMATTER))
            .orElseGet(LocalDate::now);
        Disc disc = new Disc(asin, title, discType, releaseDate);
        if (releaseDate.equals(LocalDate.now()))
            jmsMessage.warning("创建碟片时未发现发售日期，碟片=" + disc.getLogName());
        return discRepository.save(disc);
    }

}
