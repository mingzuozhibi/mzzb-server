package com.mingzuozhibi.modules.disc;

import com.mingzuozhibi.commons.gson.InstantUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.util.Comparator.*;

public interface DiscGroupRepository extends JpaRepository<DiscGroup, Long> {

    List<DiscGroup> findByViewTypeNot(DiscGroup.ViewType privateList);

    List<DiscGroup> findByEnabled(boolean enabled);

    Optional<DiscGroup> findByKey(String key);

    default void updateModifyTime() {
        Comparator<Disc> comparator = comparing(Disc::getUpdateTime, nullsFirst(naturalOrder()));
        findByEnabled(true).forEach(discGroup -> {
            discGroup.getDiscs().stream().max(comparator).ifPresent(disc -> {
                discGroup.setModifyTime(InstantUtils.toInstant(disc.getUpdateTime()));
            });
        });
    }

}
