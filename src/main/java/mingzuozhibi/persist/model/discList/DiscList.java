package mingzuozhibi.persist.model.discList;

import mingzuozhibi.persist.BaseModel;
import mingzuozhibi.persist.model.disc.Disc;
import org.springframework.util.Assert;

import javax.persistence.*;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Entity
@Table(name = "disc_list")
public class DiscList extends BaseModel implements Comparable<DiscList> {

    private String name;
    private String title;
    private boolean sakura;

    private Date date;
    private List<Disc> discs = new LinkedList<>();

    @Column(length = 100, nullable = false, unique = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(length = 100, nullable = false)
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Column
    public boolean isSakura() {
        return sakura;
    }

    public void setSakura(boolean sakura) {
        this.sakura = sakura;
    }

    @Column
    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "disc_list_discs")
    public List<Disc> getDiscs() {
        return discs;
    }

    public void setDiscs(List<Disc> discs) {
        this.discs = discs;
    }

    public int compareTo(DiscList other) {
        Assert.notNull(other);
        if (sakura != other.sakura) {
            return sakura ? -1 : 1;
        } else {
            return other.name.compareTo(name);
        }
    }

    @Transient
    public boolean isTop100() {
        return "top_100".equals(getName());
    }

    public static String titleOfSeason(String name) {
        return name.substring(0, 4) + "年" + name.substring(5) + "月新番";
    }

}
