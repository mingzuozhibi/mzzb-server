package com.mingzuozhibi.modules.core.record;

import com.mingzuozhibi.modules.core.disc.Disc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface HourRecordRepository extends JpaRepository<HourRecord, Long> {

    Optional<HourRecord> findByDiscAndDate(Disc disc, LocalDate now);

}
