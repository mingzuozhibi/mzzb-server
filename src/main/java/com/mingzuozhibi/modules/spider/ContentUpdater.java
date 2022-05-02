package com.mingzuozhibi.modules.spider;

import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.commons.mylog.JmsBind;
import com.mingzuozhibi.commons.mylog.JmsEnums.Name;
import com.mingzuozhibi.modules.disc.*;
import com.mingzuozhibi.modules.disc.Disc.DiscType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static com.mingzuozhibi.commons.utils.FormatUtils.fmtDate;

@Component
@JmsBind(Name.SERVER_DISC)
public class ContentUpdater extends BaseSupport {

    @Autowired
    private GroupService groupService;

    @Autowired
    private DiscRepository discRepository;

    @Transactional
    public void updateDiscs(List<Content> contents, Instant updateOn) {
        try {
            bind.notify("开始更新日亚排名");
            for (Content content : contents) {
                try {
                    updateDisc(content, updateOn);
                } catch (Exception e) {
                    String format = "更新碟片遇到错误：%s, json=%s";
                    bind.warning(format, e, gson.toJson(content));
                }
            }

            if (contents.size() > 0) {
                groupService.updateGroupModifyTime();
                bind.success("成功更新日亚排名：共%d个", contents.size());
            } else {
                bind.warning("未能更新日亚排名：无数据");
            }
        } catch (Exception e) {
            bind.warning("未能更新日亚排名: %s", e.getMessage());
        }
    }

    private void updateDisc(Content content, Instant updateOn) {
        String asin = content.getAsin();
        Optional<Disc> byAsin = discRepository.findByAsin(asin);
        if (!byAsin.isPresent()) {
            bind.warning("[应用碟片更新时，发现未知碟片][%s]", asin);
            return;
        }
        if (content.isOffTheShelf()) {
            bind.warning("[碟片可能已下架][%s]", asin);
            return;
        }
        Disc disc = byAsin.get();
        updateTitle(disc, content);
        updateType(disc, content);
        updateDate(disc, content);
        if (disc.getModifyTime() == null || updateOn.isAfter(disc.getModifyTime())) {
            DiscUtils.updateRank(disc, content.getRank(), updateOn);
        }
    }

    private void updateTitle(Disc disc, Content content) {
        String title = content.getTitle();
        if (!Objects.equals(title, disc.getTitle())) {
            bind.info("[碟片标题更新][%s => %s][%s]", disc.getTitle(), title, disc.getAsin());
            disc.setTitle(title);
        }
    }

    private void updateType(Disc disc, Content content) {
        DiscType type = DiscType.valueOf(content.getType());
        if (disc.getDiscType() == DiscType.Auto || disc.getDiscType() == DiscType.Other) {
            disc.setDiscType(type);
        }
        if (!Objects.equals(type, disc.getDiscType())) {
            bind.warning("[碟片类型不符][%s => %s][%s]", disc.getDiscType(), type, disc.getAsin());
        }
    }

    private void updateDate(Disc disc, Content content) {
        if (!StringUtils.hasLength(content.getDate())) {
            bind.info("[发售时间为空][当前设置为%s][%s]", disc.getReleaseDate(), disc.getAsin());
            return;
        }
        LocalDate date = LocalDate.parse(content.getDate(), fmtDate);
        boolean buyset = content.isBuyset();
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
