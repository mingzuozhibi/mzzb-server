package com.mingzuozhibi.modules.group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DiscGroupRepository extends JpaRepository<DiscGroup, Long> {

    List<DiscGroup> findByViewTypeNot(DiscGroup.ViewType privateList);

    default List<DiscGroup> findAllHasPrivate(boolean hasPrivate) {
        if (hasPrivate) {
            return findAll();
        } else {
            return findByViewTypeNot(DiscGroup.ViewType.PrivateList);
        }
    }

    List<DiscGroup> findByEnabled(boolean enabled);

    Optional<DiscGroup> findByKey(String key);

    @Query("from DiscGroup where enabled = true order by key desc")
    List<DiscGroup> findActiveDiscGroups();

}
