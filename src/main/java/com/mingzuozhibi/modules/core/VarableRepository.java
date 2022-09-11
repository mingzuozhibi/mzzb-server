package com.mingzuozhibi.modules.core;

import org.springframework.data.jpa.repository.*;

import java.util.Optional;

public interface VarableRepository extends JpaRepository<Varable, Long> {

    Optional<Varable> findByKey(String key);

    @Modifying
    @Query("update Varable set content = ?2 where key = ?1")
    void update(String key, String content);

}
