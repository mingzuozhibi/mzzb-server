package com.mingzuozhibi.persist.rank;

import com.mingzuozhibi.commons.BaseModel;
import com.mingzuozhibi.persist.disc.Disc;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;
import java.time.LocalDate;

@Entity
public class DateRecord extends BaseModel {

    private Disc disc;
    private LocalDate date;
    private Double rank;
    private Double todayPt;
    private Double totalPt;
    private Double guessPt;

    public DateRecord() {
    }

    public DateRecord(Disc disc, LocalDate date) {
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

    @Column(name = "`rank`")
    public Double getRank() {
        return rank;
    }

    public void setRank(Double rank) {
        this.rank = rank;
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

}
