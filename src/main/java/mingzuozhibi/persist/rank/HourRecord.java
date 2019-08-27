package mingzuozhibi.persist.rank;

import mingzuozhibi.persist.BaseModel;
import mingzuozhibi.persist.disc.DateRank;
import mingzuozhibi.persist.disc.Disc;

import javax.persistence.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.OptionalDouble;
import java.util.stream.IntStream;

@Entity
public class HourRecord extends BaseModel {

    private Disc disc;
    private LocalDate date;
    private DateRank rank;
    private Double todayPt;
    private Double totalPt;

    public HourRecord() {
    }

    public HourRecord(Disc disc, LocalDate date) {
        this.disc = disc;
        this.date = date;
        this.rank = new DateRank();
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

    @Embedded
    public DateRank getRank() {
        return rank;
    }

    public void setRank(DateRank rank) {
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

    private static Method[] rankSetters = new Method[24];
    private static Method[] rankGetters = new Method[24];

    static {
        for (int i = 0; i < 24; i++) {
            Class<DateRank> rankClass = DateRank.class;
            try {
                rankSetters[i] = rankClass.getMethod(String.format("setRank%02d", i), Integer.class);
                rankGetters[i] = rankClass.getMethod(String.format("getRank%02d", i));
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    }

    @Transient
    public void setRank(int hour, Integer rank) {
        if (this.rank == null) {
            this.rank = new DateRank();
        }
        try {
            rankSetters[hour].invoke(this.rank, rank);
        } catch (IllegalAccessException | InvocationTargetException ignore) {
        }
    }

    @Transient
    public Integer getRank(int hour) {
        if (this.rank == null) {
            return null;
        }
        try {
            return (Integer) rankGetters[hour].invoke(this.rank);
        } catch (IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    @Transient
    public OptionalDouble getAverRank() {
        IntStream.Builder builder = IntStream.builder();
        for (int i = 0; i < 24; i++) {
            Integer rank = getRank(i);
            if (rank != null && rank != 0) {
                builder.add(rank);
            }
        }
        return builder.build().average();
    }

}
