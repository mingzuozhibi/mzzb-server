package com.mingzuozhibi.modules.spider;

import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.commons.mylog.JmsEnums.Name;
import com.mingzuozhibi.commons.mylog.JmsLogger;
import com.mingzuozhibi.modules.disc.*;
import com.mingzuozhibi.modules.disc.Disc.DiscType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static com.mingzuozhibi.commons.utils.FormatUtils.fmtDate;

@Slf4j
@Component
public class DiscUpdater extends BaseSupport {

    private JmsLogger bind;

    @PostConstruct
    public void bind() {
        bind = jmsSender.bind(Name.SERVER_DISC);
    }

    @Autowired
    private GroupService groupService;

    @Autowired
    private DiscRepository discRepository;

    @Transactional
    public void updateDiscs(List<DiscContent> discContents, Instant updateOn) {
        try {
            bind.notify("开始更新日亚排名");
            for (DiscContent discContent : discContents) {
                try {
                    updateDisc(discContent, updateOn);
                } catch (Exception e) {
                    String format = "更新碟片遇到错误：%s, json=%s";
                    bind.warning(format, e, gson.toJson(discContent));
                }
            }

            if (discContents.size() > 0) {
                groupService.updateGroupModifyTime();
                bind.notify("成功更新日亚排名：共%d个", discContents.size());
            } else {
                bind.warning("未能更新日亚排名：无数据");
            }
        } catch (Exception e) {
            bind.warning("未能更新日亚排名: %s", e.getMessage());
        }
    }

    private void updateDisc(DiscContent discContent, Instant updateOn) {
        String asin = discContent.getAsin();
        Optional<Disc> byAsin = discRepository.findByAsin(asin);
        if (!byAsin.isPresent()) {
            bind.warning("[应用碟片更新时，发现未知碟片][%s]", asin);
            return;
        }
        if (discContent.isOffTheShelf()) {
            bind.warning("[碟片可能已下架][%s]", asin);
            return;
        }
        Disc disc = byAsin.get();
        updateTitle(disc, discContent);
        updateType(disc, discContent);
        updateDate(disc, discContent);
        if (disc.getModifyTime() == null || updateOn.isAfter(disc.getModifyTime())) {
            DiscUtils.updateRank(disc, discContent.getRank(), updateOn);
        }
    }

    private void updateTitle(Disc disc, DiscContent discContent) {
        String title = discContent.getTitle();
        if (!Objects.equals(title, disc.getTitle())) {
            bind.info("[碟片标题更新][%s => %s][%s]", disc.getTitle(), title, disc.getAsin());
            disc.setTitle(title);
        }
    }

    private void updateType(Disc disc, DiscContent discContent) {
        DiscType type = DiscType.valueOf(discContent.getType());
        if (disc.getDiscType() == DiscType.Auto || disc.getDiscType() == DiscType.Other) {
            disc.setDiscType(type);
        }
        if (!Objects.equals(type, disc.getDiscType())) {
            bind.warning("[碟片类型不符][%s => %s][%s]", disc.getDiscType(), type, disc.getAsin());
        }
    }

    private void updateDate(Disc disc, DiscContent discContent) {
        if (!StringUtils.hasLength(discContent.getDate())) {
            bind.info("[发售时间为空][当前设置为%s][%s]", disc.getReleaseDate(), disc.getAsin());
            return;
        }
        LocalDate date = LocalDate.parse(discContent.getDate(), fmtDate);
        boolean buyset = discContent.isBuyset();
        if (date.isAfter(disc.getReleaseDate()) && !buyset) {
            bind.notify("[发售时间更新][%s => %s][%s]", disc.getReleaseDate(), date, disc.getAsin());
            disc.setReleaseDate(date);
        }
        if (!Objects.equals(date, disc.getReleaseDate())) {
            if (buyset) {
                bind.info("[发售时间不符][%s => %s][%s][套装=true]", disc.getReleaseDate(), date, disc.getAsin());
            } else {
                bind.warning("[发售时间不符][%s => %s][%s][套装=false]", disc.getReleaseDate(), date, disc.getAsin());
            }
        }
    }

}
