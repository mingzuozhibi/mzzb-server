package com.mingzuozhibi.modules.disc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Comparator.*;
import static java.util.stream.Collectors.*;

@Service
public class GroupService {

    @Autowired
    private GroupRepository groupRepository;

    @Transactional
    public Set<String> findNeedUpdateAsins() {
        return findNeedUpdateDiscs()
            .map(Disc::getAsin)
            .collect(toCollection(LinkedHashSet::new));
    }

    private Stream<Disc> findNeedUpdateDiscs() {
        return groupRepository.findActiveDiscGroups().stream()
            .flatMap(discGroup -> discGroup.getDiscs().stream());
    }

    @Transactional
    public Set<String> findNeedUpdateAsinsSorted() {
        Map<String, List<Disc>> map = groupBy(findNeedUpdateDiscs());
        Set<String> asins = new LinkedHashSet<>();
        map.getOrDefault("list_1", emptyList()).stream()
            .sorted(comparing(Disc::getUpdateTime, nullsLast(naturalOrder())))
            .forEach(disc -> asins.add(disc.getAsin()));
        map.getOrDefault("list_2", emptyList())
            .forEach(disc -> asins.add(disc.getAsin()));
        map.getOrDefault("list_3", emptyList())
            .forEach(disc -> asins.add(disc.getAsin()));
        return asins;
    }

    private Map<String, List<Disc>> groupBy(Stream<Disc> discs) {
        Instant time_1 = Instant.now().minus(16, ChronoUnit.HOURS);
        Instant time_2 = Instant.now().minus(8, ChronoUnit.HOURS);
        Function<Disc, String> function = disc -> {
            if (disc.getUpdateTime() == null)
                return "list_1";
            if (disc.getUpdateTime().isBefore(time_1))
                return "list_1";
            if (disc.getUpdateTime().isBefore(time_2)) {
                return "list_2";
            } else {
                return "list_3";
            }
        };
        return discs.collect(groupingBy(function));
    }

    @Transactional
    public Set<Disc> findNeedRecordDiscs() {
        LocalDate target = LocalDate.now().minusDays(7);
        return findNeedUpdateDiscs()
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
