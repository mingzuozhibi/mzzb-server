package mingzuozhibi.persist.model.disc;

import mingzuozhibi.persist.BaseModel;
import mingzuozhibi.persist.model.DiscRank;
import mingzuozhibi.persist.model.DiscType;
import mingzuozhibi.persist.model.discSakura.DiscSakura;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.util.Assert;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.Comparator;
import java.util.Date;

@Entity
@Table(name = "disc")
public class Disc extends BaseModel implements Comparable<Disc> {

    private String asin;
    private String title;
    private String japan;
    private String sname;

    private DiscType type;
    private boolean amzver;

    private Date release;
    private DiscRank rank;
    private DiscSakura sakura;

    @Column(length = 20, nullable = false, unique = true)
    public String getAsin() {
        return asin;
    }

    public void setAsin(String asin) {
        this.asin = asin;
    }

    @Column(length = 500, nullable = false)
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Column(length = 500, nullable = false)
    public String getJapan() {
        return japan;
    }

    public void setJapan(String japan) {
        this.japan = japan;
    }

    @Column(length = 30)
    public String getSname() {
        return sname;
    }

    public void setSname(String sname) {
        this.sname = sname;
    }

    @Column(nullable = false)
    public DiscType getType() {
        return type;
    }

    public void setType(DiscType type) {
        this.type = type;
    }

    @Column
    public boolean isAmzver() {
        return amzver;
    }

    public void setAmzver(boolean amzver) {
        this.amzver = amzver;
    }

    @Column(name = "release_date")
    public Date getRelease() {
        return release;
    }

    public void setRelease(Date release) {
        this.release = release;
    }

    @OneToOne(mappedBy = "disc")
    public DiscRank getRank() {
        return rank;
    }

    public void setRank(DiscRank rank) {
        this.rank = rank;
    }

    @OneToOne(mappedBy = "disc")
    public DiscSakura getSakura() {
        return sakura;
    }

    public void setSakura(DiscSakura sakura) {
        this.sakura = sakura;
    }

    public int compareTo(Disc other) {
        Assert.notNull(other, "other must not null");
        return title.compareTo(other.title);
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        Disc disc = (Disc) obj;
        return new EqualsBuilder()
                .append(asin, disc.asin)
                .isEquals();
    }

    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(asin)
                .toHashCode();
    }

    public static Comparator<Disc> sortBySakura() {
        return (o1, o2) -> {
            if (o1.sakura != null && o2.sakura != null) {
                return o1.sakura.compareTo(o2.sakura);
            }
            if (o1.sakura == null && o2.sakura == null) {
                return o1.compareTo(o2);
            }
            return o1.sakura == null ? 1 : -1;
        };
    }

    public static Comparator<Disc> sortByAmazon() {
        return (o1, o2) -> {
            if (o1.rank != null && o2.rank != null) {
                return o1.rank.compareTo(o2.rank);
            }
            if (o1.rank == null && o2.rank == null) {
                return o1.compareTo(o2);
            }
            return o1.rank == null ? 1 : -1;
        };
    }

    public static String titleOfDisc(String discName) {
        discName = discName.replace("【Blu-ray】", " [Blu-ray]");
        discName = discName.replace("【DVD】", " [DVD]");
        if (isAmzver(discName)) {
            discName = discName.substring(16).trim() + "【尼限定】";
        }
        discName = discName.replaceAll("\\s+", " ");
        return discName;
    }

    public static boolean isAmzver(String japan) {
        return japan.startsWith("【Amazon.co.jp限定】");
    }

}
