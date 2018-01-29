package mingzuozhibi.service;

import mingzuozhibi.persist.model.disc.Disc;
import mingzuozhibi.persist.model.discList.DiscList;
import mingzuozhibi.persist.model.discList.DiscListRepository;
import mingzuozhibi.support.Dao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EveryHourTask {

    @Autowired
    private Dao dao;

    @Autowired
    private DiscListRepository discListRepository;

    @Transactional
    public void run() {
        dao.execute(session -> {
            Logger logger = LoggerFactory.getLogger(EveryHourTask.class);
            List<DiscList> sakuras = discListRepository.findBySakura(true)
                    .stream().filter(discList -> !discList.isTop100())
                    .collect(Collectors.toList());
            sakuras.forEach(discList -> {
                List<Disc> toDelete = discList.getDiscs().stream()
                        .filter(this::isReleasedTenDays)
                        .collect(Collectors.toList());
                discList.getDiscs().removeAll(toDelete);
                logger.info("remove {} discs from disclist {}", toDelete.size(), discList.getTitle());
                toDelete.forEach(disc -> logger.debug("remove disc {} from disclist {}", disc.getTitle(), discList.getTitle()));
            });
        });
    }

    private boolean isReleasedTenDays(Disc disc) {
        long nowtime = System.currentTimeMillis();
        long release = disc.getRelease().getTime();
        long tenDays = 10 * 24 * 3600 * 1000;
        return nowtime > release + tenDays;
    }

}
