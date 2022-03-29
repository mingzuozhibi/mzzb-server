package com.mingzuozhibi.persist.rank;

import com.mingzuozhibi.persist.disc.Disc;
import com.mingzuozhibi.persist.BaseModel;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.OptionalDouble;
import java.util.stream.DoubleStream;

@Entity
public class HourRecord extends BaseModel {

    private Disc disc;
    private LocalDate date;
    private Double todayPt;
    private Double totalPt;
    private Double guessPt;
    private HourRecordEmbedded rank;

    public HourRecord() {
    }

    public HourRecord(Disc disc, LocalDate date) {
        this.disc = disc;
        this.date = date;
    }

    @OneToOne(fetch = FetchType.LAZY)
    public Disc getDisc() {
        return disc;
    }

    public void setDisc(Disc disc) {
        this.disc = disc;
    }

    @Column(nullable = false)
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    @Column
    public Double getTodayPt() {
        return todayPt;
    }

    public void setTodayPt(Double todayPt) {
        this.todayPt = todayPt;
    }

    @Column
    public Double getTotalPt() {
        return totalPt;
    }

    public void setTotalPt(Double totalPt) {
        this.totalPt = totalPt;
    }

    @Column
    public Double getGuessPt() {
        return guessPt;
    }

    public void setGuessPt(Double guessPt) {
        this.guessPt = guessPt;
    }

    @Embedded
    public HourRecordEmbedded getRank() {
        return rank;
    }

    public void setRank(HourRecordEmbedded rank) {
        this.rank = rank;
    }

    @Transient
    public void setRank(int hour, Integer rank) {
        if (this.rank == null) {
            this.rank = new HourRecordEmbedded();
        }
        this.rank.setRank(hour, rank);
    }

    @Transient
    public Integer getRank(int hour) {
        if (this.rank == null) {
            return null;
        }
        return this.rank.getRank(hour);
    }

    @Transient
    public OptionalDouble getAverRank() {
        if (this.rank == null) {
            return OptionalDouble.empty();
        }
        DoubleStream.Builder builder = DoubleStream.builder();
        for (int hour = 0; hour < 24; hour++) {
            Integer rank = getRank(hour);
            if (rank != null) {
                builder.add(rank);
            }
        }
        return builder.build().average();
    }

}
