package com.mingzuozhibi.modules.disc.rank;

import com.mingzuozhibi.modules.disc.Disc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface DateRecordRepository extends JpaRepository<DateRecord, Long> {

    @Query("from Disc where disc = $1 and date < $2 order by date desc")
    List<DateRecord> findDateRecords(Disc disc, LocalDate date);

}
