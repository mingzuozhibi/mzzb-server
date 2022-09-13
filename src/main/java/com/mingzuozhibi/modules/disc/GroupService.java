package com.mingzuozhibi.modules.disc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Predicate;

import static java.util.Comparator.*;
import static java.util.stream.Collectors.toCollection;

@Service
public class GroupService {

    @Autowired
    private GroupRepository groupRepository;

    @Transactional
    public long getFetchCount() {
        return groupRepository.findActiveDiscs().stream()
            .map(Disc::getAsin)
            .distinct().count();
    }

    @Transactional
    public Set<Disc> findNeedUpdateDiscs() {
        var discs = groupRepository.findActiveDiscs();

        Predicate<Disc> isNeedQuick = disc -> disc.getUpdateTime() == null ||
            disc.getUpdateTime().isBefore(Instant.now().minus(5, ChronoUnit.HOURS));
        Predicate<Disc> isNeedFetch = disc -> disc.getUpdateTime() != null &&
            disc.getUpdateTime().isBefore(Instant.now().minus(1, ChronoUnit.HOURS));

        Set<Disc> result = new LinkedHashSet<>();
        discs.stream()
            .filter(isNeedQuick)
            .sorted(comparing(Disc::getUpdateTime, nullsLast(naturalOrder())))
            .forEach(result::add);
        discs.stream()
            .filter(isNeedFetch)
            .forEach(result::add);
        return result;
    }

    @Transactional
    public Set<Disc> findNeedRecordDiscs() {
        var target = LocalDate.now().minusDays(7);
        return groupRepository.findActiveDiscs().stream()
            .filter(disc -> disc.getReleaseDate().isAfter(target))
            .collect(toCollection(LinkedHashSet::new));
    }

    @Transactional
    public void updateUpdateOn() {
        groupRepository.findByEnabled(true).forEach(group -> {
            findLastUpdate(group).ifPresent(disc -> {
                group.setModifyTime(disc.getUpdateTime());
            });
        });
    }

    private Optional<Disc> findLastUpdate(Group group) {
        return group.getDiscs().stream()
            .max(comparing(Disc::getUpdateTime, nullsFirst(naturalOrder())));
    }

}
