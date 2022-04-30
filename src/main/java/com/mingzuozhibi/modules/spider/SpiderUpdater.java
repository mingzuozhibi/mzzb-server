package com.mingzuozhibi.modules.spider;

import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.commons.mylog.JmsEnums.Name;
import com.mingzuozhibi.commons.mylog.JmsLogger;
import com.mingzuozhibi.modules.disc.*;
import com.mingzuozhibi.modules.disc.Disc.DiscType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static com.mingzuozhibi.commons.utils.FormatUtils.fmtDate;

@Slf4j
@Service
public class SpiderUpdater extends BaseSupport {

    private JmsLogger bind;

    @PostConstruct
    public void bind() {
        bind = jmsSender.bind(Name.SERVER_DISC);
    }

    @Autowired
    private DiscRepository discRepository;

    @Autowired
    private GroupService groupService;

    @Transactional
    public void updateDiscs(List<DiscUpdate> discUpdates, Instant updateOn) {
        try {
            bind.notify("开始更新日亚排名");
            for (DiscUpdate discUpdate : discUpdates) {
                try {
                    updateDisc(discUpdate, updateOn);
                } catch (Exception e) {
                    String format = "更新碟片遇到错误：%s, json=%s";
                    bind.warning(format, e, gson.toJson(discUpdate));
                }
            }

            if (discUpdates.size() > 0) {
                groupService.updateGroupModifyTime();
                bind.notify("成功更新日亚排名：共%d个", discUpdates.size());
            } else {
                bind.warning("未能更新日亚排名：无数据");
            }
        } catch (Exception e) {
            bind.warning("未能更新日亚排名: %s", e.getMessage());
        }
    }

    private void updateDisc(DiscUpdate discUpdate, Instant updateOn) {
        String asin = discUpdate.getAsin();
        Optional<Disc> byAsin = discRepository.findByAsin(asin);
        if (!byAsin.isPresent()) {
            bind.warning("[应用碟片更新时，发现未知碟片][%s]", asin);
            return;
        }
        Disc disc = byAsin.get();
        if (discUpdate.isOffTheShelf()) {
            bind.warning("[碟片可能已下架][%s]", asin);
            return;
        }
        updateTitle(disc, discUpdate);
        updateType(disc, discUpdate);
        updateDate(disc, discUpdate);
        updateRank(disc, discUpdate, updateOn);
    }

    private void updateTitle(Disc disc, DiscUpdate discUpdate) {
        String title = discUpdate.getTitle();
        if (!Objects.equals(title, disc.getTitle())) {
            bind.info("[碟片标题更新][%s => %s][%s]", disc.getTitle(), title, disc.getAsin());
            disc.setTitle(title);
        }
    }

    private void updateType(Disc disc, DiscUpdate discUpdate) {
        DiscType type = DiscType.valueOf(discUpdate.getType());
        if (disc.getDiscType() == DiscType.Auto || disc.getDiscType() == DiscType.Other) {
            disc.setDiscType(type);
        }
        if (!Objects.equals(type, disc.getDiscType())) {
            bind.warning("[碟片类型不符][%s => %s][%s]", disc.getDiscType(), type, disc.getAsin());
        }
    }

    private void updateDate(Disc disc, DiscUpdate discUpdate) {
        if (!StringUtils.hasLength(discUpdate.getDate())) {
            bind.info("[发售时间为空][当前设置为%s][%s]", disc.getReleaseDate(), disc.getAsin());
            return;
        }
        LocalDate date = LocalDate.parse(discUpdate.getDate(), fmtDate);
        boolean buyset = discUpdate.isBuyset();
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

    private void updateRank(Disc disc, DiscUpdate discUpdate, Instant updateOn) {
        if (disc.getModifyTime() == null || updateOn.isAfter(disc.getModifyTime())) {
            updateRank(disc, discUpdate.getRank(), updateOn);
        }
    }

    public static void updateRank(Disc disc, Integer rank, Instant updateOn) {
        disc.setPrevRank(disc.getThisRank());
        disc.setThisRank(rank);
        disc.setUpdateTime(updateOn);
        if (!Objects.equals(disc.getThisRank(), disc.getPrevRank())) {
            disc.setModifyTime(updateOn);
        }
    }

}
