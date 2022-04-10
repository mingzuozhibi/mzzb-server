package com.mingzuozhibi.modules.core.record;

import com.google.gson.JsonObject;
import com.mingzuozhibi.modules.core.disc.Disc;

import java.time.LocalDate;

public interface DiscRecord {

    Long getId();

    Disc getDisc();

    LocalDate getDate();

    Double getAverRank();

    Double getTodayPt();

    Double getTotalPt();

    Double getGuessPt();

    JsonObject toJson();

}
