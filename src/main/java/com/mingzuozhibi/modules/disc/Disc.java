package com.mingzuozhibi.modules.disc;

import com.mingzuozhibi.commons.base.BaseModel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.json.JSONObject;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Disc extends BaseModel implements Comparable<Disc> {

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

    public JSONObject toJSON() {
        JSONObject object = new JSONObject();
        object.put("id", getId());
        object.put("asin", getAsin());
        object.put("title", getTitle());
        object.put("titlePc", getTitlePc());
        object.put("thisRank", getThisRank());
        object.put("prevRank", getPrevRank());
        object.put("nicoBook", getNicoBook());
        object.put("todayPt", getTodayPt());
        object.put("totalPt", getTotalPt());
        object.put("guessPt", getGuessPt());
        object.put("discType", getDiscType().name());
        object.put("releaseDate", getReleaseDate().toString());
        object.put("surplusDays", getSurplusDays());
        object.put("createTime", toEpochMilli(getCreateTime()));
        Optional.ofNullable(getUpdateTime()).ifPresent(updateTime -> {
            object.put("updateTime", toEpochMilli(updateTime));
        });
        Optional.ofNullable(getModifyTime()).ifPresent(modifyTime -> {
            object.put("modifyTime", toEpochMilli(modifyTime));
        });
        return object;
    }

    public JSONObject toJSON(Set<String> columns) {
        JSONObject object = toJSON();
        new HashSet<>(object.keySet()).stream()
            .filter(key -> !columns.contains(key))
            .forEach(object::remove);
        return object;
    }

}
