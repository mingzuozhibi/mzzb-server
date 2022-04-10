package com.mingzuozhibi.modules.disc;

import com.google.gson.JsonObject;

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
