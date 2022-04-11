package com.mingzuozhibi.modules.spider;

import com.google.gson.reflect.TypeToken;
import com.mingzuozhibi.commons.base.BaseController2;
import com.mingzuozhibi.commons.mylog.JmsService;
import com.mingzuozhibi.modules.disc.Disc;
import com.mingzuozhibi.modules.disc.Disc.DiscType;
import com.mingzuozhibi.modules.disc.DiscRepository;
import com.mingzuozhibi.modules.disc.DiscService;
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
    private DiscService discService;

    @Autowired
    private DiscRepository discRepository;

    @Scheduled(cron = "0 59 * * * ?")
    @GetMapping("/admin/sendNeedUpdateAsins")
    public void sendNeedUpdateAsins() {
        Set<String> asins = discService.findNeedUpdateAsins();
        jmsService.convertAndSend("need.update.asins", gson.toJson(asins));
        jmsMessage.notify("JMS -> need.update.asins size=" + asins.size());
    }

    @JmsListener(destination = "prev.update.discs")
    public void discSpiderUpdate(String json) {
        TypeToken<?> typeToken = TypeToken.getParameterized(ArrayList.class, DiscUpdate.class);
        List<DiscUpdate> discUpdates = gson.fromJson(json, typeToken.getType());
        jmsMessage.notify("JMS <- prev.update.discs size=" + discUpdates.size());
        discSpider.applyDiscUpdates(discUpdates);
    }

    @JmsListener(destination = "done.update.discs")
    public void discSpiderUpdate2(String json) {
        TypeToken<?> typeToken = TypeToken.getParameterized(ArrayList.class, DiscUpdate.class);
        List<DiscUpdate> discUpdates = gson.fromJson(json, typeToken.getType());
        jmsMessage.notify("JMS <- done.update.discs size=" + discUpdates.size());
        discSpider.applyDiscUpdates(discUpdates);
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @GetMapping(value = "/api/admin/fetchCount")
    public String getFetchCount() {
        return dataResult(discService.findNeedUpdateAsins().size());
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @GetMapping(value = "/api/admin/searchDisc/{asin}", produces = MEDIA_TYPE)
    public String searchDisc(@PathVariable String asin) {
        Optional<Disc> byAsin = discRepository.findByAsin(asin);
        if (byAsin.isPresent()) {
            return dataResult(byAsin.get().toJson());
        }
        return searchDiscFromAmazon(asin);
    }

    private String searchDiscFromAmazon(@PathVariable String asin) {
        SearchTask<DiscUpdate> task = discSpider.runSearchTask(asin);
        if (!task.isSuccess()) {
            return errorResult(task.getMessage());
        }
        DiscUpdate discUpdate = task.getData();
        if (discUpdate.isOffTheShelf()) {
            return errorResult("可能该碟片已下架");
        }
        Disc disc = createDisc(asin, discUpdate);
        jmsService.sendDiscTrack(disc.getAsin(), disc.getTitle());
        jmsMessage.success(logCreate("碟片", disc.getTitle(), disc.getLogName()));
        return dataResult(disc);
    }

    private Disc createDisc(@PathVariable String asin, DiscUpdate discUpdate) {
        String title = discUpdate.getTitle();
        DiscType discType = DiscType.valueOf(discUpdate.getType());
        LocalDate releaseDate = Optional.ofNullable(discUpdate.getDate())
            .map(date -> LocalDate.parse(date, DATE_FORMATTER))
            .orElseGet(LocalDate::now);
        Disc disc = new Disc(asin, title, discType, releaseDate);
        if (releaseDate.equals(LocalDate.now()))
            jmsMessage.warning("创建碟片时未发现发售日期，碟片=" + disc.getLogName());
        return discRepository.save(disc);
    }

}
