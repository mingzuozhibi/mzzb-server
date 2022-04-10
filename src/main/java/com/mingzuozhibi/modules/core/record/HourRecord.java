package com.mingzuozhibi.modules.core.record;

import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.base.BaseModel2;
import com.mingzuozhibi.commons.gson.GsonIgnored;
import com.mingzuozhibi.modules.core.disc.Disc;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.DoubleStream;

import static com.mingzuozhibi.commons.gson.GsonFactory.GSON;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class HourRecord extends BaseModel2 implements DiscRecord {

    public HourRecord(Disc disc, LocalDate date) {
        this.disc = disc;
        this.date = date;
    }

    @OneToOne(fetch = FetchType.LAZY)
    @GsonIgnored
    private Disc disc;

    @Column(nullable = false)
    private LocalDate date;

    @Column
    private Double todayPt;

    @Column
    private Double totalPt;

    @Column
    private Double guessPt;

    @Embedded
    @GsonIgnored
    private HourRecordEmbedded embedded;

    public void setRank(int hour, Integer rank) {
        if (this.embedded == null) {
            this.embedded = new HourRecordEmbedded();
        }
        this.embedded.setRank(hour, rank);
    }

    @Transient
    public Integer getRank(int hour) {
        if (this.embedded == null) {
            return null;
        }
        return this.embedded.getRank(hour);
    }

    @Transient
    public Double getAverRank() {
        if (this.embedded == null) {
            return null;
        }
        DoubleStream.Builder builder = DoubleStream.builder();
        for (int hour = 0; hour < 24; hour++) {
            Integer rank = getRank(hour);
            if (rank != null) {
                builder.add(rank);
            }
        }
        OptionalDouble average = builder.build().average();
        return average.isPresent() ? average.getAsDouble() : null;
    }

    public JsonObject toJson() {
        JsonObject object = GSON.toJsonTree(this).getAsJsonObject();
        Optional.ofNullable(getAverRank()).ifPresent(rank -> {
            object.addProperty("rank", rank);
        });
        return object;
    }

}
