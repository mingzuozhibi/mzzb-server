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
        DiscGroupUtils.findActiveDiscGroups(session).forEach(discGroup -> {
            for (Disc disc : discGroup.getDiscs()) {
                if (disc.getReleaseDate().isAfter(stopRecordDate)) {
                    discs.add(disc);
                }
            }
        });

        return discs;
    }

    public static Set<String> needUpdateAsins(Session session) {
        Set<String> asins = new LinkedHashSet<>();
        DiscGroupUtils.findActiveDiscGroups(session).forEach(discGroup -> {
            for (Disc disc : discGroup.getDiscs()) {
                asins.add(disc.getAsin());
            }
        });
        return asins;
    }

}
