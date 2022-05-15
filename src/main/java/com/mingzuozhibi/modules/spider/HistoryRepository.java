package com.mingzuozhibi.modules.spider;

import org.springframework.data.jpa.repository.*;

import java.util.Optional;

public interface HistoryRepository extends JpaRepository<History, Long> {

    @Modifying
    @Query(value = "update history " +
        "set tracked = ?2 " +
        "where asin = ?1", nativeQuery = true)
    void setTracked(String asin, boolean tracked);

    Optional<History> findByAsin(String asin);

}
