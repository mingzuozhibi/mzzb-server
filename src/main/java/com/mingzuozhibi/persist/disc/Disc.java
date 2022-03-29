package com.mingzuozhibi.persist.disc;

import com.mingzuozhibi.persist.BaseModel;
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
public class Disc extends BaseModel implements Comparable<Disc> {

    public enum DiscType {
        Cd, Dvd, Bluray, Auto, Other
    }

    private String asin;
    private String title;
    private String titlePc;
    private Integer thisRank;
    private Integer prevRank;
    private Integer nicoBook;
    private Integer todayPt;
    private Integer totalPt;
    private Integer guessPt;
    private DiscType discType;
    private LocalDate releaseDate;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime modifyTime;

    public Disc() {
    }

    public Disc(String asin, String title, DiscType discType, LocalDate releaseDate) {
        this.asin = asin;
        this.title = title;
        this.discType = discType;
        this.releaseDate = releaseDate;
        this.createTime = LocalDateTime.now().withNano(0);
    }

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

    @Column(length = 500)
    public String getTitlePc() {
        return titlePc;
    }

    public void setTitlePc(String titlePc) {
        this.titlePc = titlePc;
    }

    @Column
    public Integer getThisRank() {
        return thisRank;
    }

    public void setThisRank(Integer thisRank) {
        this.thisRank = thisRank;
    }

    @Column
    public Integer getPrevRank() {
        return prevRank;
    }

    public void setPrevRank(Integer prevRank) {
        this.prevRank = prevRank;
    }

    @Column
    public Integer getNicoBook() {
        return nicoBook;
    }

    public void setNicoBook(Integer nicoBook) {
        this.nicoBook = nicoBook;
    }

    @Column
    public Integer getTodayPt() {
        return todayPt;
    }

    public void setTodayPt(Integer todayPt) {
        this.todayPt = todayPt;
    }

    @Column
    public Integer getTotalPt() {
        return totalPt;
    }

    public void setTotalPt(Integer totalPt) {
        this.totalPt = totalPt;
    }

    @Column
    public Integer getGuessPt() {
        return guessPt;
    }

    public void setGuessPt(Integer guessPt) {
        this.guessPt = guessPt;
    }

    @Column(nullable = false)
    public DiscType getDiscType() {
        return discType;
    }

    public void setDiscType(DiscType discType) {
        this.discType = discType;
    }

    @Column(nullable = false)
    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    @Column(nullable = false)
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Column
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @Column
    public LocalDateTime getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(LocalDateTime modifyTime) {
        this.modifyTime = modifyTime;
    }

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
