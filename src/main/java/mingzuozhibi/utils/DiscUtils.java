package mingzuozhibi.utils;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.DiscGroup;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class DiscUtils {

    public static Set<Disc> needRecordDiscs(Session session) {
        Set<Disc> discs = new LinkedHashSet<>();

        LocalDate stopRecordDate = LocalDate.now().minusDays(7);
        findActiveDiscGroups(session).forEach(discGroup -> {
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
        findActiveDiscGroups(session).forEach(discGroup -> {
            for (Disc disc : discGroup.getDiscs()) {
                asins.add(disc.getAsin());
            }
        });
        return asins;
    }

    @SuppressWarnings("unchecked")
    private static List<DiscGroup> findActiveDiscGroups(Session session) {
        return session.createCriteria(DiscGroup.class)
                .add(Restrictions.eq("enabled", true))
                .addOrder(Order.desc("key"))
                .list();
    }

}
