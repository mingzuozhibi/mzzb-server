package com.mingzuozhibi.modules.record;

import com.google.gson.JsonObject;
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

    JsonObject toJson();

    void setTodayPt(Double pt);

    void setTotalPt(Double pt);

    void setGuessPt(Double pt);

}
