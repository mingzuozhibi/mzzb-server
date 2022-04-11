package com.mingzuozhibi.modules.disc;

import com.mingzuozhibi.commons.gson.adapter.AdapterUtils;
import com.mingzuozhibi.modules.group.DiscGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Comparator.*;

@Service
public class DiscService {

    @Autowired
    private DiscGroupRepository discGroupRepository;

    public Set<String> findNeedUpdateAsins() {
        Set<String> asins = new LinkedHashSet<>();
        discGroupRepository.findActiveDiscGroups().forEach(discGroup -> {
            discGroup.getDiscs().forEach(disc -> {
                asins.add(disc.getAsin());
            });
        });
        return asins;
    }

    public void updateGroupModifyTime() {
        Comparator<Disc> comparator = comparing(Disc::getUpdateTime, nullsFirst(naturalOrder()));
        discGroupRepository.findByEnabled(true).forEach(discGroup -> {
            discGroup.getDiscs().stream().max(comparator).ifPresent(disc -> {
                discGroup.setModifyTime(AdapterUtils.toInstant(disc.getUpdateTime()));
            });
        });
    }

}
