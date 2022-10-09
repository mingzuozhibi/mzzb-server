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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static com.mingzuozhibi.commons.utils.LoggerUtils.logWarn;
import static com.mingzuozhibi.commons.utils.MyTimeUtils.fmtDate;
import static com.mingzuozhibi.modules.disc.DiscUtils.updateRank;

@Slf4j
@Component
@LoggerBind(Name.SERVER_DISC)
public class ContentUpdater extends BaseSupport {

    @Autowired
    private DiscRepository discRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Transactional
    public long updateAllContent(List<Content> contents, Instant updateOn) {
        var proxy = (ContentUpdater) AopContext.currentProxy();
        var updateCount = new AtomicLong(0);
        for (var content : contents) {
            try {
                proxy.updateContent(content, updateOn);
                updateCount.incrementAndGet();
            } catch (Exception e) {
                bind.warning("updateContent(content=%s) throws %s".formatted(
                    gson.toJson(content), e.toString()));
                log.debug("updateContent", e);
            }
        }
        groupRepository.updateModifyTime();
        return updateCount.get();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateContent(Content content, Instant updateOn) {
        var asin = content.getAsin();
        if (content.isLogoff()) {
            bind.warning("[碟片可能已下架][%s]".formatted(asin));
            return;
        }

        var byAsin = discRepository.findByAsin(asin);
        if (byAsin.isEmpty()) {
            bind.warning("[应用碟片更新时，发现未知碟片][%s]".formatted(asin));
            return;
        }

        var disc = byAsin.get();
        updateTitle(disc, content);
        updateType(disc, content);
        updateDate(disc, content);
        updateRank(disc, content.getRank(), updateOn);
    }

    private void updateTitle(Disc disc, Content content) {
        var title = content.getTitle();
        if (title.length() > 500) {
            bind.warning("[碟片标题过长][length=%d][title=%s]".formatted(title.length(), title));
            title = title.substring(0, 500);
        }
        if (!Objects.equals(title, disc.getTitle())) {
            bind.debug("[碟片标题更新][%s => %s][%s]".formatted(disc.getTitle(), title, disc.getAsin()));
            disc.setTitle(title);
        }
    }

    private void updateType(Disc disc, Content content) {
        var type = DiscType.valueOf(content.getType());
        if (type != disc.getDiscType() && disc.getDiscType() == DiscType.Auto) {
            disc.setDiscType(type);
        }
        if (type != disc.getDiscType()) {
            logWarn(bind, type == DiscType.Auto, "[碟片类型不符][%s => %s][%s]".formatted(
                disc.getDiscType(), type, disc.getAsin()));
        }
    }

    private void updateDate(Disc disc, Content content) {
        var asin = disc.getAsin();
        var buyset = content.isBuyset();

        if (!StringUtils.hasLength(content.getDate())) {
            bind.debug("[发售时间为空][当前设置为%s][%s][套装=%b][类型=%s]".formatted(
                disc.getReleaseDate(), asin, buyset, disc.getDiscType()));
            return;
        }

        var date = LocalDate.parse(content.getDate(), fmtDate);
        if (date.isAfter(disc.getReleaseDate())) {
            bind.notify("[发售时间更新][%s => %s][%s]".formatted(disc.getReleaseDate(), date, asin));
            disc.setReleaseDate(date);
        }

        if (!Objects.equals(date, disc.getReleaseDate())) {
            bind.warning("[发售时间不符][%s => %s][%s][套装=%b][类型=%s]".formatted(
                disc.getReleaseDate(), date, asin, buyset, disc.getDiscType()));
        }
    }

}
