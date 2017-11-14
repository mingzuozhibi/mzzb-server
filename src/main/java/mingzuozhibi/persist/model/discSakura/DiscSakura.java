package mingzuozhibi.persist.model.discSakura;

import mingzuozhibi.persist.BaseModel;
import mingzuozhibi.persist.model.disc.Disc;
import org.springframework.util.Assert;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Table(name = "disc_sakura")
public class DiscSakura extends BaseModel implements Comparable<DiscSakura> {

    private Disc disc;

    private Date date;
    private int curk; // current rank
    private int prrk; // previous rank
    private int cubk; // current book
    private int cupt; // current point
    private int sday; // surplus days

    @OneToOne(optional = false)
    public Disc getDisc() {
        return disc;
    }

    public void setDisc(Disc disc) {
        this.disc = disc;
    }

    @Column
    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Column
    public int getCurk() {
        return curk;
    }

    public void setCurk(int curk) {
        this.curk = curk;
    }

    @Column
    public int getPrrk() {
        return prrk;
    }

    public void setPrrk(int prrk) {
        this.prrk = prrk;
    }

    @Column
    public int getCubk() {
        return cubk;
    }

    public void setCubk(int cubk) {
        this.cubk = cubk;
    }

    @Column
    public int getCupt() {
        return cupt;
    }

    public void setCupt(int cupt) {
        this.cupt = cupt;
    }

    @Column
    public int getSday() {
        return sday;
    }

    public void setSday(int sday) {
        this.sday = sday;
    }

    public int compareTo(DiscSakura other) {
        Assert.notNull(other);
        if (curk != 0 && other.curk != 0) {
            return curk - other.curk;
        } else {
            return other.curk - curk;
        }
    }

}
