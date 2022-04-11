package com.mingzuozhibi.modules.group;

import com.mingzuozhibi.modules.disc.Disc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mingzuozhibi.utils.MyTimeUtils.toInstant;
import static java.util.Comparator.*;

@Service
public class DiscGroupService {

    @Autowired
    private DiscGroupRepository discGroupRepository;

    @Transactional
    public Set<String> findNeedUpdateAsins() {
        return discGroupRepository.findActiveDiscGroups().stream()
            .flatMap(discGroup -> discGroup.getDiscs().stream().map(Disc::getAsin))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Transactional
    public Set<Disc> findNeedRecordDiscs() {
        LocalDate expirationDate = LocalDate.now().minusDays(7);
        return discGroupRepository.findActiveDiscGroups().stream()
            .flatMap(discGroup -> discGroup.getDiscs().stream())
            .filter(disc -> disc.getReleaseDate().isAfter(expirationDate))
            .collect(Collectors.toCollection(LinkedHashSet::new));
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
