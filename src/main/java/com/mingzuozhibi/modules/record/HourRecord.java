package com.mingzuozhibi.modules.record;

import com.mingzuozhibi.commons.base.BaseEntity;
import com.mingzuozhibi.commons.gson.GsonIgnored;
import com.mingzuozhibi.modules.disc.Disc;
import lombok.*;

import javax.persistence.*;
import java.io.Serial;
import java.time.LocalDate;
import java.util.stream.DoubleStream;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class HourRecord extends BaseEntity implements Record {

    @Serial
    private static final long serialVersionUID = 100L;

    public HourRecord(Disc disc, LocalDate date) {
        this.disc = disc;
        this.date = date;
    }

    @GsonIgnored
    @OneToOne(fetch = FetchType.LAZY)
    private Disc disc;

    @Column(nullable = false)
    private LocalDate date;

    @Column
    private Double todayPt;

    @Column
    private Double totalPt;

    @Column
    private Double guessPt;

    @Transient
    public Double getAverRank() {
        return calculateRank();
    }

    @Embedded
    @GsonIgnored
    private HourRecordEmbedded embedded;

    public void setRank(int hour, Integer rank) {
        if (this.embedded == null) {
            this.embedded = new HourRecordEmbedded();
        }
        this.embedded.setRank(hour, rank);
    }

    public Integer getRank(int hour) {
        if (this.embedded == null) {
            return null;
        }
        return this.embedded.getRank(hour);
    }

    private Double calculateRank() {
        if (this.embedded == null) {
            return null;
        }
        var builder = DoubleStream.builder();
        for (var hour = 0; hour < 24; hour++) {
            var rank = getRank(hour);
            if (rank != null) {
                builder.add(rank);
            }
        }
        var average = builder.build().average();
        return average.isPresent() ? average.getAsDouble() : null;
    }

}
