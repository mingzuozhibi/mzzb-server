package com.mingzuozhibi.modules.core.record;

import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.base.BaseModel2;
import com.mingzuozhibi.commons.gson.GsonIgnored;
import com.mingzuozhibi.modules.core.disc.Disc;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;
import java.time.LocalDate;

import static com.mingzuozhibi.commons.gson.GsonFactory.GSON;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class DateRecord extends BaseModel2 implements DiscRecord {

    public DateRecord(Disc disc, LocalDate date) {
        this.disc = disc;
        this.date = date;
    }

    @OneToOne(fetch = FetchType.LAZY)
    @GsonIgnored
    private Disc disc;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "`rank`")
    private Double rank;

    @Column
    private Double todayPt;

    @Column
    private Double totalPt;

    @Column
    private Double guessPt;

    @Override
    public Double getAverRank() {
        return rank;
    }

    @Override
    public JsonObject toJson() {
        return GSON.toJsonTree(this).getAsJsonObject();
    }

}
