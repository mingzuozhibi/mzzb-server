package com.mingzuozhibi.modules.core;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VarableRepository extends JpaRepository<Varable, Long> {

    Optional<Varable> findByKey(String key);

}
