package mingzuozhibi.utils;

import mingzuozhibi.persist.disc.Disc;
import org.hibernate.Session;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

public abstract class DiscUtils {

    public static Set<Disc> needRecordDiscs(Session session) {
        Set<Disc> discs = new LinkedHashSet<>();

        SakuraUtils.sakurasOrderByDescKey(session).forEach(sakura -> {
            sakura.getDiscs().stream().filter(needRecordDisc()).forEach(discs::add);
        });

        return discs;
    }

    public static Set<String> needUpdateAsins(Session session) {
        Set<String> asins = new LinkedHashSet<>();
        Set<String> first = new LinkedHashSet<>();
        LocalDateTime tenHours = LocalDateTime.now().minusHours(10);

        SakuraUtils.sakurasOrderByDescKey(session).forEach(sakura -> {
            sakura.getDiscs().stream()
                    .filter(needUpdateDisc())
                    .forEach(disc -> {
                        LocalDateTime lastUpdate = disc.getUpdateTime();
                        if (lastUpdate == null || lastUpdate.isBefore(tenHours)) {
                            first.add(disc.getAsin());
                        } else {
                            asins.add(disc.getAsin());
                        }
                    });
        });

        first.addAll(asins);
        return first;
    }

    private static Predicate<Disc> needRecordDisc() {
        return disc -> {
            LocalDate stopRecord = disc.getReleaseDate().plusDays(7);
            return disc.getUpdateType().isNeedRecord() && LocalDate.now().isBefore(stopRecord);
        };
    }

    private static Predicate<Disc> needUpdateDisc() {
        return disc -> disc.getUpdateType().isNeedUpdate();
    }

}
