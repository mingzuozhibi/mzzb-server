package mingzuozhibi.persist.model;

import mingzuozhibi.persist.BaseModel;
import mingzuozhibi.persist.model.disc.Disc;
import org.springframework.util.Assert;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "disc_record")
public class DiscRecord extends BaseModel implements Comparable<DiscRecord> {

    private Disc disc;
    private Date date; // used field: yyyy-MM-dd-HH
    private int rank; // this hour rank
    private double adpt;
    private double cupt;

    @ManyToOne(optional = false)
    public Disc getDisc() {
        return disc;
    }

    public void setDisc(Disc disc) {
        this.disc = disc;
    }

    @Column(nullable = false)
    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Column
    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    @Transient
    public double getAdpt() {
        return adpt;
    }

    public void setAdpt(double adpt) {
        this.adpt = adpt;
    }

    @Transient
    public double getCupt() {
        return cupt;
    }

    public void setCupt(double cupt) {
        this.cupt = cupt;
    }

    public int compareTo(DiscRecord other) {
        Assert.notNull(other);
        return other.date.compareTo(date);
    }

}
