package com.mingzuozhibi.modules.record;

import com.mingzuozhibi.commons.base.BaseEntity;
import com.mingzuozhibi.commons.gson.GsonIgnored;
import com.mingzuozhibi.modules.disc.Disc;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class DateRecord extends BaseEntity implements Record {

    private static final long serialVersionUID = 100L;

    public DateRecord(Disc disc, LocalDate date) {
        this.disc = disc;
        this.date = date;
    }

    @GsonIgnored
    @OneToOne(fetch = FetchType.LAZY)
    private Disc disc;

    @Column(nullable = false)
    private LocalDate date;

    @GsonIgnored
    @Column
    private Double rank;

    @Column
    private Double todayPt;

    @Column
    private Double totalPt;

    @Column
    private Double guessPt;

    @Transient
    public Double getAverRank() {
        return rank;
    }

}
