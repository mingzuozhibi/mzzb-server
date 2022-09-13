package com.mingzuozhibi.modules.disc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static java.util.stream.Collectors.toCollection;

@Transactional
public interface DiscRepository extends JpaRepository<Disc, Long> {

    Optional<Disc> findByAsin(String asin);

    boolean existsByAsin(String asin);

    @Query(value = "select count(*) from group_discs " +
        "where group_id = ?1", nativeQuery = true)
    long countByGroup(Group group);

    @Query(value = "select count(*) from group_discs " +
        "where group_id = ?1 and disc_id = ?2", nativeQuery = true)
    long countByGroup(Group group, Disc disc);

    @Query(value = "Select Distinct d " +
        "From Group g " +
        "Join g.discs d " +
        "Where g.enabled = true")
    List<Disc> findActiveDiscs();

    @Query(value = "Select Count(Distinct d) " +
        "From Group g " +
        "Join g.discs d " +
        "Where g.enabled = true")
    long countActiveDiscs();

    default Set<Disc> findNeedUpdate() {
        var activeDiscs = findActiveDiscs();
        var needQuick = Instant.now().minus(5, ChronoUnit.HOURS);
        var needFetch = Instant.now().minus(1, ChronoUnit.HOURS);
        return DiscUtils.findNeedUpdate(activeDiscs, needQuick, needFetch);
    }

    default Set<Disc> findNeedRecord() {
        var needRecord = LocalDate.now().minusDays(7);
        return findActiveDiscs().stream()
            .filter(disc -> disc.getReleaseDate().isAfter(needRecord))
            .collect(toCollection(LinkedHashSet::new));
    }

}
