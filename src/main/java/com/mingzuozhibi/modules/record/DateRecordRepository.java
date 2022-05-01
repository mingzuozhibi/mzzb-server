package com.mingzuozhibi.modules.record;

import com.mingzuozhibi.modules.disc.Disc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface DateRecordRepository extends JpaRepository<DateRecord, Long> {

    @Query(value = "select * from date_record " +
        "where disc = ?1 and date < ?2 " +
        "order by date desc", nativeQuery = true)
    List<DateRecord> findDateRecords(Disc disc, LocalDate date);

    DateRecord getByDiscAndDate(Disc disc, LocalDate date);

    List<DateRecord> findByDiscOrderByDate(Disc disc);

    List<DateRecord> findByDate(LocalDate date);

    @Query(value = "select * from date_record " +
        "where disc_id = ?1 and date < ?2 " +
        "order by date desc limit 1", nativeQuery = true)
    DateRecord getLastDateRecord(Disc disc, LocalDate date);

}
