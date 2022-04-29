package com.mingzuozhibi.modules.record;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mingzuozhibi.commons.base.BaseEntity;
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

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    private Disc disc;

    @Column(nullable = false)
    private LocalDate date;

    @JsonIgnore
    @Column(name = "`rank`")
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
