package com.mingzuozhibi.modules.disc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface DiscRepository extends JpaRepository<Disc, Long> {

    boolean existsByAsin(String asin);

    Optional<Disc> findByAsin(String asin);

    @Query(value = "select count(*) from disc_group_discs where disc_group_id = ?1", nativeQuery = true)
    long countGroupDiscs(Long groupId);

    @Query(value = "select count(*) from disc_group_discs where disc_group_id = ?1 and disc_id = ?2", nativeQuery = true)
    long countGroupDiscs(Long groupId, Long discId);

    default boolean existsDiscInGroup(Group group, Disc disc) {
        return countGroupDiscs(group.getId(), disc.getId()) > 0;
    }

}
