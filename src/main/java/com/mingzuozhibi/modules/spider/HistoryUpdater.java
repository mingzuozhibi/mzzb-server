package com.mingzuozhibi.modules.spider;

import com.mingzuozhibi.commons.base.BaseKeys.Name;
import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.commons.logger.Logger;
import com.mingzuozhibi.commons.logger.LoggerBind;
import com.mingzuozhibi.modules.disc.DiscRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@LoggerBind(Name.SPIDER_HISTORY)
public class HistoryUpdater extends BaseSupport {

    @Autowired
    private DiscRepository discRepository;

    @Autowired
    private HistoryRepository historyRepository;

    @Transactional
    public void updateAllHistory(List<History> histories) {
        Logger logger = amqpSender.bind(Name.SPIDER_HISTORY);
        AtomicInteger count = new AtomicInteger(0);
        histories.forEach(history -> {
            Optional<History> byAsin = historyRepository.findByAsin(history.getAsin());
            if (byAsin.isEmpty()) {
                history.setTracked(discRepository.existsByAsin(history.getAsin()));
                historyRepository.save(history);
                logger.success("[发现新碟片][asin=%s][type=%s][title=%s]".formatted(
                    history.getAsin(), history.getType(), history.getTitle()));
                count.incrementAndGet();
            } else {
                History toUpdate = byAsin.get();
                toUpdate.setType(history.getType());
                toUpdate.setTitle(history.getTitle());
            }
        });
        logger.notify("发现新碟片%d个".formatted(count.get()));
    }

}
