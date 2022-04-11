package com.mingzuozhibi.modules.disc;

import com.mingzuozhibi.commons.gson.adapter.AdapterUtils;
import com.mingzuozhibi.modules.group.DiscGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Comparator.*;

@Service
public class DiscService {

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
                discGroup.setModifyTime(AdapterUtils.toInstant(disc.getUpdateTime()));
            });
        });
    }

}
