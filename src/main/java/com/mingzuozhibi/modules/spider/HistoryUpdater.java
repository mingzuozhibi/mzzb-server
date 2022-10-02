package com.mingzuozhibi.modules.spider;

import com.mingzuozhibi.commons.base.BaseKeys.Name;
import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.commons.logger.LoggerBind;
import com.mingzuozhibi.modules.disc.DiscRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@LoggerBind(Name.SPIDER_HISTORY)
public class HistoryUpdater extends BaseSupport {

    @Autowired
    private DiscRepository discRepository;

    @Autowired
    private HistoryRepository historyRepository;

    @Transactional
    public void updateAllHistory(List<History> histories) {
        var proxy = (HistoryUpdater) AopContext.currentProxy();
        var addNewCount = new AtomicLong(0);
        for (var history : histories) {
            try {
                if (proxy.updateHistory(history)) {
                    addNewCount.incrementAndGet();
                }
            } catch (Exception e) {
                bind.warning("updateHistory(history=%s) throws %s".formatted(
                    gson.toJson(history), e.toString()
                ));
                log.debug("updateHistory", e);
            }
        }
        bind.success("发现新碟片，共%d个".formatted(addNewCount.get()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean updateHistory(History history) {
        var byAsin = historyRepository.findByAsin(history.getAsin());
        if (byAsin.isPresent()) {
            var toUpdate = byAsin.get();
            toUpdate.setType(history.getType());
            if (history.getDate() != null) {
                toUpdate.setDate(history.getDate());
            }
            toUpdate.setTitle(history.getTitle());
            return false;
        } else {
            history.setTracked(discRepository.existsByAsin(history.getAsin()));
            historyRepository.save(history);
            bind.notify("[发现新碟片][asin=%s][type=%s][date=%s][title=%s]".formatted(
                history.getAsin(), history.getType(), history.getDate(), history.getTitle()));
            return true;
        }
    }

}
