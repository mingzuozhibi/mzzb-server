package mingzuozhibi.service;

import mingzuozhibi.persist.Disc;
import mingzuozhibi.persist.Sakura;
import mingzuozhibi.support.Dao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EveryHourTask {

    @Autowired
    private Dao dao;

    @Transactional
    public void run() {
        dao.execute(session -> {
            List<Sakura> sakuras = dao.findAll(Sakura.class).stream()
                    .filter(sakura -> !sakura.isTop100())
                    .collect(Collectors.toList());
            sakuras.forEach(sakura -> {
                List<Disc> toDelete = sakura.getDiscs().stream()
                        .filter(this::isReleasedTenDays)
                        .collect(Collectors.toList());
                sakura.getDiscs().removeAll(toDelete);

                Logger logger = LoggerFactory.getLogger(EveryHourTask.class);
                if (logger.isInfoEnabled() && toDelete.size() > 0) {
                    logger.info("从列表[{}]移除{}个碟片", sakura.getTitle(), toDelete.size());
                    toDelete.forEach(disc -> logger.info("移除碟片{}", disc.getTitle()));
                } else {
                    logger.info("列表[{}]没有需要移除的过期碟片", sakura.getTitle());
                }
            });
        });
    }

    private boolean isReleasedTenDays(Disc disc) {
        long nowtime = LocalDate.now().toEpochDay();
        long release = disc.getReleaseDate().toEpochDay();
        return nowtime > release + 10;
    }

}
