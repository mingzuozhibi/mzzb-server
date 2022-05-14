package com.mingzuozhibi.modules.disc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface DiscRepository extends JpaRepository<Disc, Long> {

    boolean existsByAsin(String asin);

    Optional<Disc> findByAsin(String asin);

    @Query(value = "select count(*) from group_discs " +
        "where group_id = ?1", nativeQuery = true)
    long countByGroup(Group group);

    @Query(value = "select count(*) from group_discs " +
        "where group_id = ?1 and disc_id = ?2", nativeQuery = true)
    long countByGroup(Group group, Disc disc);

}
