package mingzuozhibi.persist.model;

import mingzuozhibi.persist.BaseModel;
import mingzuozhibi.persist.model.disc.Disc;
import org.springframework.util.Assert;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Table(name = "disc_rank")
public class DiscRank extends BaseModel implements Comparable<DiscRank> {

    private Disc disc;

    private Date spdt; // speed date
    private int sprk; // speed rank

    private Date padt; // page date
    private int park; // page rank

    private Date padt1;
    private int park1;

    private Date padt2;
    private int park2;

    private Date padt3;
    private int park3;

    private Date padt4;
    private int park4;

    private Date padt5;
    private int park5;

    @OneToOne(optional = false)
    public Disc getDisc() {
        return disc;
    }

    public void setDisc(Disc disc) {
        this.disc = disc;
    }

    @Column
    public Date getSpdt() {
        return spdt;
    }

    public void setSpdt(Date spdt) {
        this.spdt = spdt;
    }

    @Column
    public int getSprk() {
        return sprk;
    }

    public void setSprk(int sprk) {
        this.sprk = sprk;
    }

    @Column
    public Date getPadt() {
        return padt;
    }

    public void setPadt(Date padt) {
        this.padt = padt;
    }

    @Column
    public int getPark() {
        return park;
    }

    public void setPark(int park) {
        this.park = park;
    }

    @Column
    public Date getPadt1() {
        return padt1;
    }

    public void setPadt1(Date padt1) {
        this.padt1 = padt1;
    }

    @Column
    public int getPark1() {
        return park1;
    }

    public void setPark1(int park1) {
        this.park1 = park1;
    }

    @Column
    public Date getPadt2() {
        return padt2;
    }

    public void setPadt2(Date padt2) {
        this.padt2 = padt2;
    }

    @Column
    public int getPark2() {
        return park2;
    }

    public void setPark2(int park2) {
        this.park2 = park2;
    }

    @Column
    public Date getPadt3() {
        return padt3;
    }

    public void setPadt3(Date padt3) {
        this.padt3 = padt3;
    }

    @Column
    public int getPark3() {
        return park3;
    }

    public void setPark3(int park3) {
        this.park3 = park3;
    }

    @Column
    public Date getPadt4() {
        return padt4;
    }

    public void setPadt4(Date padt4) {
        this.padt4 = padt4;
    }

    @Column
    public int getPark4() {
        return park4;
    }

    public void setPark4(int park4) {
        this.park4 = park4;
    }

    @Column
    public Date getPadt5() {
        return padt5;
    }

    public void setPadt5(Date padt5) {
        this.padt5 = padt5;
    }

    @Column
    public int getPark5() {
        return park5;
    }

    public void setPark5(int park5) {
        this.park5 = park5;
    }

    public int compareTo(DiscRank other) {
        Assert.notNull(other, "other must not null");
        if (park != 0 && other.park != 0) {
            return park - other.park;
        } else {
            return other.park - park;
        }
    }

}
