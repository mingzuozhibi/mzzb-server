package com.mingzuozhibi.modules.spider;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HistoryRepository extends JpaRepository<History, Long> {

    Optional<History> findByAsin(String asin);

    default void setTracked(String asin, boolean tracked) {
        findByAsin(asin).ifPresent(history -> history.setTracked(tracked));
    }

}
