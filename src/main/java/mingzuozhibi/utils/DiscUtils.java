package mingzuozhibi.utils;

import mingzuozhibi.persist.disc.Disc;
import org.hibernate.Session;

import java.time.LocalDate;
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

        SakuraUtils.sakurasOrderByDescKey(session).forEach(sakura -> {
            sakura.getDiscs().stream()
                    .filter(needUpdateDisc())
                    .map(Disc::getAsin)
                    .forEach(asins::add);
        });
        return asins;
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
