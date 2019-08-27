package mingzuozhibi.utils;

import mingzuozhibi.persist.disc.Disc;
import org.hibernate.Session;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class DiscUtils {

    public static Set<Disc> needRecordDiscs(Session session) {
        Set<Disc> discs = new LinkedHashSet<>();

        LocalDate stopRecordDate = LocalDate.now().minusDays(7);
        SakuraUtils.sakurasOrderByDescKey(session).forEach(sakura -> {
            for (Disc disc : sakura.getDiscs()) {
                if (disc.getReleaseDate().isAfter(stopRecordDate)) {
                    discs.add(disc);
                }
            }
        });

        return discs;
    }

    public static Set<String> needUpdateAsins(Session session) {
        Set<String> asins = new LinkedHashSet<>();
        SakuraUtils.sakurasOrderByDescKey(session).forEach(sakura -> {
            for (Disc disc : sakura.getDiscs()) {
                asins.add(disc.getAsin());
            }
        });
        return asins;
    }

}
