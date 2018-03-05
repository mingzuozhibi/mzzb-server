package mingzuozhibi.persist.disc;

import mingzuozhibi.persist.BaseModel;

import javax.persistence.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;

@Entity
public class Record extends BaseModel {

    private Disc disc;
    private DateRank rank;
    private LocalDate date;

    public Record() {
    }

    public Record(Disc disc, LocalDate date) {
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

    @Embedded
    public DateRank getRank() {
        return rank;
    }

    public void setRank(DateRank rank) {
        this.rank = rank;
    }

    @Column(nullable = false)
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    private static Method[] rankSetters = new Method[24];
    private static Method[] rankGetters = new Method[24];

    static {
        for (int i = 0; i < 24; i++) {
            Class<DateRank> rankClass = DateRank.class;
            try {
                rankSetters[i] = rankClass.getMethod(String.format("setRank%02d", i + 1), Integer.class);
                rankGetters[i] = rankClass.getMethod(String.format("getRank%02d", i + 1));
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    }

    @Transient
    public void setRank(int hour, Integer rank) {
        if (rank == null) {
            return;
        }
        try {
            rankSetters[hour - 1].invoke(this.rank, rank);
        } catch (IllegalAccessException | InvocationTargetException ignore) {
        }
    }

    @Transient
    public Integer getRank(int hour) {
        try {
            return (Integer) rankGetters[hour - 1].invoke(this.rank);
        } catch (IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

}
