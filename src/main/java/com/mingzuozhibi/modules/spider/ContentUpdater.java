package com.mingzuozhibi.modules.spider;

import com.mingzuozhibi.commons.base.BaseKeys.Name;
import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.commons.logger.LoggerBind;
import com.mingzuozhibi.modules.disc.*;
import com.mingzuozhibi.modules.disc.Disc.DiscType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static com.mingzuozhibi.commons.utils.FormatUtils.fmtDate;
import static com.mingzuozhibi.modules.disc.DiscUtils.updateRank;

@Slf4j
@Component
@LoggerBind(Name.SERVER_DISC)
public class ContentUpdater extends BaseSupport {

    @Autowired
    private GroupService groupService;

    @Autowired
    private DiscRepository discRepository;

    @Transactional
    public long updateAllContent(List<Content> contents, Instant updateOn) {
        var proxy = (ContentUpdater) AopContext.currentProxy();
        var updateCount = new AtomicLong(0);
        for (Content content : contents) {
            try {
                proxy.updateContent(content, updateOn);
                updateCount.incrementAndGet();
            } catch (Exception e) {
                bind.warning("updateContent(content=%s) throws %s".formatted(
                    gson.toJson(content), e.toString()));
                log.debug("updateContent", e);
            }
        }
        groupService.updateUpdateOn();
        return updateCount.get();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateContent(Content content, Instant updateOn) {
        String asin = content.getAsin();
        if (content.isOffTheShelf()) {
            bind.warning("[碟片可能已下架][%s]".formatted(asin));
            return;
        }

        Optional<Disc> byAsin = discRepository.findByAsin(asin);
        if (byAsin.isEmpty()) {
            bind.warning("[应用碟片更新时，发现未知碟片][%s]".formatted(asin));
            return;
        }

        Disc disc = byAsin.get();
        updateTitle(disc, content);
        updateType(disc, content);
        updateDate(disc, content);
        updateRank(disc, content.getRank(), updateOn);
    }

    private void updateTitle(Disc disc, Content content) {
        String title = content.getTitle();
        if (!Objects.equals(title, disc.getTitle())) {
            bind.debug("[碟片标题更新][%s => %s][%s]".formatted(disc.getTitle(), title, disc.getAsin()));
            disc.setTitle(title);
        }
    }

    private void updateType(Disc disc, Content content) {
        DiscType type = DiscType.valueOf(content.getType());
        if (disc.getDiscType() == DiscType.Auto || disc.getDiscType() == DiscType.Other) {
            disc.setDiscType(type);
        }
        if (!Objects.equals(type, disc.getDiscType())) {
            bind.warning("[碟片类型不符][%s => %s][%s]".formatted(disc.getDiscType(), type, disc.getAsin()));
        }
    }

    private void updateDate(Disc disc, Content content) {
        String asin = disc.getAsin();
        boolean buyset = content.isBuyset();

        if (!StringUtils.hasLength(content.getDate())) {
            logBuyset(buyset, "[发售时间为空][当前设置为%s][%s][套装=%b]".formatted(
                disc.getReleaseDate(), asin, buyset));
            return;
        }

        LocalDate date = LocalDate.parse(content.getDate(), fmtDate);
        if (date.isAfter(disc.getReleaseDate()) && !buyset) {
            bind.notify("[发售时间更新][%s => %s][%s]".formatted(disc.getReleaseDate(), date, asin));
            disc.setReleaseDate(date);
        }

        if (!Objects.equals(date, disc.getReleaseDate())) {
            logBuyset(buyset, "[发售时间不符][%s => %s][%s][套装=%b]".formatted(
                disc.getReleaseDate(), date, asin, buyset));
        }
    }

    private void logBuyset(boolean buyset, String formatted) {
        if (buyset) {
            bind.debug(formatted);
        } else {
            bind.warning(formatted);
        }
    }

}
