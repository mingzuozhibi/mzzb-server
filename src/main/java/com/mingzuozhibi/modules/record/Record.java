package com.mingzuozhibi.modules.record;

import com.mingzuozhibi.modules.disc.Disc;

import java.time.LocalDate;

public interface Record {

    Long getId();

    Disc getDisc();

    LocalDate getDate();

    Double getAverRank();

    Double getTodayPt();

    Double getTotalPt();

    Double getGuessPt();

    void setTodayPt(Double pt);

    void setTotalPt(Double pt);

    void setGuessPt(Double pt);

}
