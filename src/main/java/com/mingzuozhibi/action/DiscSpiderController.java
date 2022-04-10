package com.mingzuozhibi.action;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mingzuozhibi.commons.base.BaseController;
import com.mingzuozhibi.commons.mylog.JmsMessage;
import com.mingzuozhibi.commons.mylog.JmsService;
import com.mingzuozhibi.modules.core.disc.Disc;
import com.mingzuozhibi.modules.core.disc.Disc.DiscType;
import com.mingzuozhibi.modules.core.disc.DiscRepository;
import com.mingzuozhibi.modules.spider.DiscSpider;
import com.mingzuozhibi.modules.spider.DiscUpdate;
import com.mingzuozhibi.modules.spider.SearchTask;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mingzuozhibi.commons.utils.ModifyUtils.logCreate;
import static com.mingzuozhibi.utils.DiscUtils.needUpdateAsins;

@RestController
public class DiscSpiderController extends BaseController {

    @Autowired
    private Gson gson;

    @Autowired
    private JmsService jmsService;

    @Autowired
    private JmsMessage jmsMessage;

    @Autowired
    private DiscSpider discSpider;

    @Autowired
    private DiscRepository discRepository;

    @Scheduled(cron = "0 59 * * * ?")
    @GetMapping("/admin/sendNeedUpdateAsins")
    public void sendNeedUpdateAsins() {
        dao.execute(session -> {
            JSONArray array = new JSONArray();
            needUpdateAsins(session).forEach(array::put);
            jmsService.convertAndSend("need.update.asins", array.toString());
        });
    }

    @JmsListener(destination = "prev.update.discs")
    public void discSpiderUpdate(String json) {
        TypeToken<?> typeToken = TypeToken.getParameterized(ArrayList.class, DiscUpdate.class);
        List<DiscUpdate> discUpdates = gson.fromJson(json, typeToken.getType());
        LOGGER.info("JMS <- prev.update.discs size=" + discUpdates.size());
        discSpider.applyDiscUpdates(discUpdates);
    }

    @JmsListener(destination = "done.update.discs")
    public void discSpiderUpdate2(String json) {
        TypeToken<?> typeToken = TypeToken.getParameterized(ArrayList.class, DiscUpdate.class);
        List<DiscUpdate> discUpdates = gson.fromJson(json, typeToken.getType());
        LOGGER.info("JMS <- done.update.discs size=" + discUpdates.size());
        discSpider.applyDiscUpdates(discUpdates);
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @GetMapping(value = "/api/admin/fetchCount")
    public String fetchCount() {
        int fetchCount = needUpdateAsins(dao.session()).size();
        debugRequest("[fetchCount:{}]", fetchCount);
        return objectResult(fetchCount);
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @GetMapping(value = "/api/admin/searchDisc/{asin}", produces = MEDIA_TYPE)
    public String searchDisc(@PathVariable String asin) {
        Optional<Disc> byAsin = discRepository.findByAsin(asin);
        if (byAsin.isPresent()) {
            return objectResult(byAsin.get().toJSON());
        }
        return searchDiscFromAmazon(asin);
    }

    private String searchDiscFromAmazon(@PathVariable String asin) {
        if (LOGGER.isInfoEnabled()) {
            infoRequest("[申请查询碟片][开始从日亚查询][ASIN={}]", asin);
        }
        SearchTask<DiscUpdate> task = discSpider.runSearchTask(asin);
        if (!task.isSuccess()) {
            return errorMessage(task.getMessage());
        }
        DiscUpdate discUpdate = task.getData();
        if (discUpdate.isOffTheShelf()) {
            return errorMessage("可能该碟片已下架");
        }

        Disc disc = createDisc(asin, discUpdate);
        jmsService.sendDiscTrack(disc.getAsin(), disc.getTitle());
        jmsMessage.success(logCreate("碟片", disc.getTitle(), asin));

        JSONObject data = disc.toJSON();
        return objectResult(data);
    }

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private Disc createDisc(@PathVariable String asin, DiscUpdate discUpdate) {
        String title = discUpdate.getTitle();
        DiscType discType = DiscType.valueOf(discUpdate.getType());
        LocalDate releaseDate = Optional.ofNullable(discUpdate.getDate())
            .map(date -> LocalDate.parse(date, formatter))
            .orElseGet(LocalDate::now);
        Disc disc = new Disc(asin, title, discType, releaseDate);
        return discRepository.save(disc);
    }

}
