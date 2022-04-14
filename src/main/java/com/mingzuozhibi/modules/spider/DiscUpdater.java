package com.mingzuozhibi.modules.spider;

import com.mingzuozhibi.commons.mylog.JmsMessage;
import com.mingzuozhibi.modules.disc.Disc;
import com.mingzuozhibi.modules.disc.Disc.DiscType;
import com.mingzuozhibi.modules.disc.DiscRepository;
import com.mingzuozhibi.modules.group.DiscGroupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.mingzuozhibi.commons.gson.GsonFactory.GSON;
import static com.mingzuozhibi.utils.FormatUtils.fmtDate;

@Slf4j
@Component
public class DiscUpdater {

    @Autowired
    private JmsMessage jmsMessage;

    @Autowired
    private DiscRepository discRepository;

    @Autowired
    private DiscGroupService discGroupService;

    @Transactional
    public void updateDiscs(List<DiscInfo> discInfos, LocalDateTime time) {
        try {
            jmsMessage.notify("开始更新日亚排名");
            for (DiscInfo discInfo : discInfos) {
                try {
                    updateDisc(discInfo, time);
                } catch (Exception e) {
                    jmsMessage.warning("更新碟片遇到错误：%s, json=%s",
                        e.toString(), GSON.toJson(discInfo));
                }
            }

            if (discInfos.size() > 0) {
                discGroupService.updateGroupModifyTime();
                jmsMessage.notify("成功更新日亚排名：共%d个", discInfos.size());
            } else {
                jmsMessage.notify("未能更新日亚排名：无数据");
            }
        } catch (Exception e) {
            jmsMessage.warning("未能更新日亚排名：%s", e.getMessage());
        }
    }

    private void updateDisc(DiscInfo discInfo, LocalDateTime updateOn) {
        String asin = discInfo.getAsin();
        Optional<Disc> byAsin = discRepository.findByAsin(asin);
        if (!byAsin.isPresent()) {
            jmsMessage.warning("[应用碟片更新时，发现未知碟片][%s]", asin);
            return;
        }
        Disc disc = byAsin.get();
        if (discInfo.isOffTheShelf()) {
            jmsMessage.warning("[碟片可能已下架][%s]", asin);
            return;
        }
        updateTitle(disc, discInfo);
        updateType(disc, discInfo);
        updateDate(disc, discInfo);
        updateRank(disc, discInfo, updateOn);
    }

    private void updateTitle(Disc disc, DiscInfo discInfo) {
        String title = discInfo.getTitle();
        if (!Objects.equals(title, disc.getTitle())) {
            jmsMessage.info("[碟片标题更新][%s => %s][%s]", disc.getTitle(), title, disc.getAsin());
            disc.setTitle(title);
        }
    }

    private void updateType(Disc disc, DiscInfo discInfo) {
        DiscType type = DiscType.valueOf(discInfo.getType());
        if (disc.getDiscType() == DiscType.Auto || disc.getDiscType() == DiscType.Other) {
            disc.setDiscType(type);
        }
        if (!Objects.equals(type, disc.getDiscType())) {
            jmsMessage.warning("[碟片类型不符][%s => %s][%s]", disc.getDiscType(), type, disc.getAsin());
        }
    }

    private void updateDate(Disc disc, DiscInfo discInfo) {
        if (!StringUtils.hasLength(discInfo.getDate())) {
            jmsMessage.info("[发售时间为空][当前设置为%s][%s]", disc.getReleaseDate(), disc.getAsin());
            return;
        }
        LocalDate date = LocalDate.parse(discInfo.getDate(), fmtDate);
        boolean buyset = discInfo.isBuyset();
        if (date.isAfter(disc.getReleaseDate()) && !buyset) {
            jmsMessage.info("[发售时间更新][%s => %s][%s]", disc.getReleaseDate(), date, disc.getAsin());
            disc.setReleaseDate(date);
        }
        if (!Objects.equals(date, disc.getReleaseDate())) {
            if (buyset) {
                jmsMessage.info("[发售时间不符][%s => %s][%s][套装=true]", disc.getReleaseDate(), date, disc.getAsin());
            } else {
                jmsMessage.warning("[发售时间不符][%s => %s][%s][套装=false]", disc.getReleaseDate(), date, disc.getAsin());
            }
        }
    }

    private void updateRank(Disc disc, DiscInfo discInfo, LocalDateTime updateOn) {
        if (disc.getModifyTime() == null || updateOn.isAfter(disc.getModifyTime())) {
            disc.setPrevRank(disc.getThisRank());
            disc.setThisRank(discInfo.getRank());
            if (!Objects.equals(disc.getThisRank(), disc.getPrevRank())) {
                disc.setModifyTime(updateOn);
            }
            disc.setUpdateTime(updateOn);
        }
    }

}
