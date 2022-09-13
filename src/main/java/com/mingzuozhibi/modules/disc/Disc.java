package com.mingzuozhibi.modules.disc;

import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.base.BaseEntity;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;
import java.io.Serial;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.Objects;

import static com.mingzuozhibi.commons.gson.GsonFactory.GSON;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Disc extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 100L;

    @SuppressWarnings("unused")
    public enum DiscType {
        Cd, Dvd, Bluray, Auto, Other
    }

    public Disc(String asin, String title, DiscType discType, LocalDate releaseDate) {
        this.asin = asin;
        this.title = title;
        this.discType = discType;
        this.releaseDate = releaseDate;
        this.createTime = Instant.now().with(ChronoField.NANO_OF_SECOND, 0L);
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
    private Instant createTime;

    @Column
    private Instant updateTime;

    @Column
    private Instant modifyTime;

    @Transient
    public String getLogName() {
        if (StringUtils.isNotBlank(titlePc)) {
            return "(%s)%s".formatted(asin, titlePc);
        } else {
            return "(%s)%s".formatted(asin, title);
        }
    }

    @Transient
    public long getSurplusDays() {
        return getReleaseDate().toEpochDay() - LocalDate.now().toEpochDay();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var disc = (Disc) o;
        return Objects.equals(asin, disc.asin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(asin);
    }

    public JsonObject toJson() {
        var object = GSON.toJsonTree(this).getAsJsonObject();
        object.addProperty("surplusDays", this.getSurplusDays());
        return object;
    }

}
