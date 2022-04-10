package com.mingzuozhibi.modules.core.disc;

import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.base.BaseModel2;
import com.mingzuozhibi.commons.gson.GsonFactory;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Disc extends BaseModel2 implements Comparable<Disc> {

    public enum DiscType {
        Cd, Dvd, Bluray, Auto, Other
    }

    public Disc(String asin, String title, DiscType discType, LocalDate releaseDate) {
        this.asin = asin;
        this.title = title;
        this.discType = discType;
        this.releaseDate = releaseDate;
        this.createTime = LocalDateTime.now().withNano(0);
    }

    @Column(length = 20, nullable = false, unique = true)
    private String asin;

    @Column(length = 500, nullable = false)
    private String title;

    @Column(length = 500)
    private String titlePc;

    @Column
    private Integer thisRank;

    @Column
    private Integer prevRank;

    @Column
    private Integer nicoBook;

    @Column
    private Integer todayPt;

    @Column
    private Integer totalPt;

    @Column
    private Integer guessPt;

    @Column(nullable = false)
    private DiscType discType;

    @Column(nullable = false)
    private LocalDate releaseDate;

    @Column(nullable = false)
    private LocalDateTime createTime;

    @Column
    private LocalDateTime updateTime;

    @Column
    private LocalDateTime modifyTime;

    @Transient
    public String getLogName() {
        return String.format("(%s)%s", asin, Optional.ofNullable(titlePc).orElse(title));
    }

    @Transient
    public long getSurplusDays() {
        return getReleaseDate().toEpochDay() - LocalDate.now().toEpochDay();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Disc disc = (Disc) o;
        return Objects.equals(asin, disc.asin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(asin);
    }

    @Override
    public int compareTo(Disc disc) {
        Objects.requireNonNull(disc);
        Integer rank1 = this.getThisRank();
        Integer rank2 = disc.getThisRank();
        boolean empty1 = rank1 == null;
        boolean empty2 = rank2 == null;
        if (!empty1 && !empty2) {
            return rank1.compareTo(rank2);
        } else {
            return empty1 && empty2 ? 0 : empty1 ? 1 : -1;
        }
    }

    public JsonObject toJson() {
        JsonObject object = GsonFactory.GSON.toJsonTree(this).getAsJsonObject();
        object.addProperty("surplusDays", this.getSurplusDays());
        return object;
    }

}
