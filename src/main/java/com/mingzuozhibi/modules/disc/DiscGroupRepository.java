package com.mingzuozhibi.modules.disc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DiscGroupRepository extends JpaRepository<DiscGroup, Long> {

    @Query(value = "select count(*) from disc_group_discs where disc_group_id = ?1", nativeQuery = true)
    long countDiscsById(Long id);

    List<DiscGroup> findByViewTypeNot(DiscGroup.ViewType privateList);

    Optional<DiscGroup> findByKey(String key);

}
