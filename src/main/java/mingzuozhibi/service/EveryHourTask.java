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
    public static final Logger LOGGGER = LoggerFactory.getLogger(EveryHourTask.class);

    @Transactional
    public void run() {
        doEveryHourTask();
    }

    private void doEveryHourTask() {
        dao.execute(session -> {
            List<Sakura> sakuras = dao.findAll(Sakura.class).stream()
                    .filter(sakura -> !sakura.isTop100())
                    .collect(Collectors.toList());
            sakuras.forEach(sakura -> {
                List<Disc> toDelete = sakura.getDiscs().stream()
                        .filter(this::isReleasedTenDays)
                        .collect(Collectors.toList());
                sakura.getDiscs().removeAll(toDelete);

                if (LOGGGER.isInfoEnabled() && toDelete.size() > 0) {
                    LOGGGER.info("从列表[{}]移除{}个碟片", sakura.getTitle(), toDelete.size());
                    toDelete.forEach(disc -> LOGGGER.info("移除碟片{}", disc.getTitle()));
                } else {
                    LOGGGER.debug("列表[{}]没有需要移除的过期碟片", sakura.getTitle());
                }
            });
        });
    }

    private boolean isReleasedTenDays(Disc disc) {
        LocalDate releaseTenDays = disc.getReleaseDate().plusDays(10);
        return LocalDate.now().isAfter(releaseTenDays);
    }

}
