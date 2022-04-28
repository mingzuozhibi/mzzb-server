package com.mingzuozhibi.modules.disc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findByViewTypeNot(Group.ViewType privateList);

    default List<Group> findAllHasPrivate(boolean hasPrivate) {
        if (hasPrivate) {
            return findAll();
        } else {
            return findByViewTypeNot(Group.ViewType.PrivateList);
        }
    }

    List<Group> findByEnabled(boolean enabled);

    Optional<Group> findByKey(String key);

    @Query(value = "select * from disc_group where enabled = true order by `key` desc", nativeQuery = true)
    List<Group> findActiveDiscGroups();

}
