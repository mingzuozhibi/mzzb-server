package mingzuozhibi.persist.disc;

import mingzuozhibi.persist.BaseModel;
import org.json.JSONObject;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Entity
public class Disc extends BaseModel implements Comparable<Disc> {

    public enum DiscType {
        Cd, Dvd, Bluray, Box, Other
    }

    public enum UpdateType {
        Sakura, Amazon, Both, None
    }

    private String asin;
    private String title;
    private String titlePc;
    private String titleMo;
    private Integer thisRank;
    private Integer prevRank;
    private Integer nicoBook;
    private Integer todayPt;
    private Integer totalPt;
    private Integer guessPt;
    private DiscType discType;
    private boolean amazonLimit;
    private UpdateType updateType;
    private LocalDate releaseDate;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime modifyTime;

    public Disc() {
    }

    public Disc(String asin, String title, DiscType discType, UpdateType updateType,
                boolean amazonLimit, LocalDate releaseDate) {
        this.asin = asin;
        this.title = title;
        this.discType = discType;
        this.updateType = updateType;
        this.amazonLimit = amazonLimit;
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

    @Column(length = 100)
    public String getTitleMo() {
        return titleMo;
    }

    public void setTitleMo(String titleMo) {
        this.titleMo = titleMo;
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
    public boolean isAmazonLimit() {
        return amazonLimit;
    }

    public void setAmazonLimit(boolean amazonLimit) {
        this.amazonLimit = amazonLimit;
    }

    @Column(nullable = false)
    public UpdateType getUpdateType() {
        return updateType;
    }

    public void setUpdateType(UpdateType updateType) {
        this.updateType = updateType;
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
        object.put("titleMo", getTitleMo());
        object.put("thisRank", getThisRank());
        object.put("prevRank", getPrevRank());
        object.put("nicoBook", getNicoBook());
        object.put("todayPt", getTodayPt());
        object.put("totalPt", getTotalPt());
        object.put("guessPt", getGuessPt());
        object.put("amazonLimit", isAmazonLimit());
        object.put("discType", getDiscType().name());
        object.put("updateType", getUpdateType().name());
        object.put("releaseDate", getReleaseDate().toString());
        object.put("createTime", toEpochMilli(getCreateTime()));
        Optional.ofNullable(getUpdateTime()).ifPresent(updateTime -> {
            object.put("updateTime", toEpochMilli(updateTime));
        });
        Optional.ofNullable(getModifyTime()).ifPresent(modifyTime -> {
            object.put("modifyTime", toEpochMilli(modifyTime));
        });
        object.put("surplusDays", getSurplusDays());
        return object;
    }

    public JSONObject toJSON(Set<String> columns) {
        JSONObject object = new JSONObject();
        if (columns.contains("id"))
            object.put("id", getId());
        if (columns.contains("asin"))
            object.put("asin", getAsin());
        if (columns.contains("title"))
            object.put("title", getTitle());
        if (columns.contains("titlePc"))
            object.put("titlePc", getTitlePc());
        if (columns.contains("titleMo"))
            object.put("titleMo", getTitleMo());
        if (columns.contains("thisRank"))
            object.put("thisRank", getThisRank());
        if (columns.contains("prevRank"))
            object.put("prevRank", getPrevRank());
        if (columns.contains("nicoBook"))
            object.put("nicoBook", getNicoBook());
        if (columns.contains("todayPt"))
            object.put("todayPt", getTodayPt());
        if (columns.contains("totalPt"))
            object.put("totalPt", getTotalPt());
        if (columns.contains("guessPt"))
            object.put("guessPt", getGuessPt());
        if (columns.contains("amazonLimit"))
            object.put("amazonLimit", isAmazonLimit());
        if (columns.contains("discType"))
            object.put("discType", getDiscType().name());
        if (columns.contains("updateType"))
            object.put("updateType", getUpdateType().name());
        if (columns.contains("releaseDate"))
            object.put("releaseDate", getReleaseDate().toString());
        if (columns.contains("createTime"))
            object.put("createTime", toEpochMilli(getCreateTime()));
        if (columns.contains("updateTime"))
            Optional.ofNullable(getUpdateTime()).ifPresent(updateTime -> {
                object.put("updateTime", toEpochMilli(updateTime));
            });
        if (columns.contains("mofidyTime"))
            Optional.ofNullable(getModifyTime()).ifPresent(modifyTime -> {
                object.put("modifyTime", toEpochMilli(modifyTime));
            });
        if (columns.contains("surplusDays"))
            object.put("surplusDays", getSurplusDays());
        return object;
    }

}
