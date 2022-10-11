package com.mingzuozhibi.modules.spider;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface HistoryRepository extends JpaRepository<History, Long>, JpaSpecificationExecutor<History> {

    Optional<History> findByAsin(String asin);

    default void setTracked(String asin, boolean tracked) {
        findByAsin(asin).ifPresent(history -> history.setTracked(tracked));
    }

}
