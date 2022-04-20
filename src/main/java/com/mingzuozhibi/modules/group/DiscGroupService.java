package com.mingzuozhibi.modules.group;

import com.mingzuozhibi.modules.disc.Disc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.mingzuozhibi.utils.MyTimeUtils.toInstant;
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
        Map<String, List<Disc>> map = findNeedUpdateDiscs()
            .collect(groupingBy(updateBeforeTarget()));
        Stream<Disc> before = map.get("before").stream()
            .sorted(comparing(Disc::getUpdateTime, nullsLast(naturalOrder())));
        Stream<Disc> normal = map.get("normal").stream();
        return Stream.concat(before, normal)
            .map(Disc::getAsin)
            .collect(toCollection(LinkedHashSet::new));
    }

    private Function<Disc, String> updateBeforeTarget() {
        LocalDateTime target = LocalDateTime.now().minusHours(15);
        return disc -> disc.getUpdateTime() != null && disc.getUpdateTime().isAfter(target)
            ? "before"
            : "normal";
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
        Comparator<Disc> comparator = comparing(Disc::getUpdateTime, nullsFirst(naturalOrder()));
        discGroupRepository.findByEnabled(true).forEach(discGroup -> {
            discGroup.getDiscs().stream().max(comparator).ifPresent(disc -> {
                discGroup.setModifyTime(toInstant(disc.getUpdateTime()));
            });
        });
    }

}
