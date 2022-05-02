package com.mingzuozhibi.modules.spider;

import org.springframework.data.jpa.repository.*;

public interface HistoryRepository extends JpaRepository<History, Long> {

    @Modifying
    @Query(value = "update history set tracked = ?2 where asin = ?1", nativeQuery = true)
    int setTracked(String asin, boolean tracked);

    boolean existsByAsin(String asin);

}
