package com.mingzuozhibi.modules.disc;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DiscRepository extends JpaRepository<Disc, Long> {

    Optional<Disc> findByAsin(String asin);
}
