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
        Set<Disc> result = new LinkedHashSet<>();

        List<Disc> discs = groupRepository.findActiveDiscs();

        Instant h6ago = Instant.now().minus(6, ChronoUnit.HOURS);
        Predicate<Disc> isNeedQuickFetch = disc ->
            disc.getUpdateTime() == null || disc.getUpdateTime().isBefore(h6ago);
        discs.stream().filter(isNeedQuickFetch)
            .sorted(comparing(Disc::getUpdateTime, nullsLast(naturalOrder())))
            .forEach(result::add);

        Instant h2ago = Instant.now().minus(2, ChronoUnit.HOURS);
        Predicate<Disc> isNeedAfterFetch = disc ->
            disc.getUpdateTime() != null && disc.getUpdateTime().isBefore(h2ago);
        discs.stream().filter(isNeedAfterFetch)
            .forEach(result::add);

        return result;
    }

    @Transactional
    public Set<Disc> findNeedRecordDiscs() {
        LocalDate target = LocalDate.now().minusDays(7);
        return groupRepository.findActiveDiscs().stream()
            .filter(disc -> disc.getReleaseDate().isAfter(target))
            .collect(toCollection(LinkedHashSet::new));
    }

    @Transactional
    public void updateGroupModifyTime() {
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
