package com.mingzuozhibi.modules.group;

import com.mingzuozhibi.modules.disc.Disc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.mingzuozhibi.commons.utils.MyTimeUtils.toInstant;
import static java.util.Collections.emptyList;
import static java.util.Comparator.*;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toCollection;

@Service
public class DiscGroupService {

    @Autowired
    private DiscGroupRepository discGroupRepository;

    @Transactional
    public Set<String> findNeedUpdateAsins() {
        return findNeedUpdateDiscs()
            .map(Disc::getAsin)
            .collect(toCollection(LinkedHashSet::new));
    }

    private Stream<Disc> findNeedUpdateDiscs() {
        return discGroupRepository.findActiveDiscGroups().stream()
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
        LocalDateTime list_1 = LocalDateTime.now().minusHours(12);
        LocalDateTime list_2 = LocalDateTime.now().minusHours(8);
        Function<Disc, String> function = disc -> {
            if (disc.getUpdateTime() == null)
                return "list_1";
            if (disc.getUpdateTime().isBefore(list_1))
                return "list_1";
            if (disc.getUpdateTime().isBefore(list_2)) {
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
        discGroupRepository.findByEnabled(true).forEach(group -> {
            group.getDiscs().stream()
                .max(comparing(Disc::getUpdateTime, nullsFirst(naturalOrder())))
                .ifPresent(disc -> {
                    group.setModifyTime(toInstant(disc.getUpdateTime()));
                });
        });
    }

}
