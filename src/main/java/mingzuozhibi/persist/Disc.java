package mingzuozhibi.persist;

import org.json.JSONObject;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Entity
public class Disc extends BaseModel {

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
    private Integer totalPt;
    private DiscType discType;
    private boolean amazonLimit;
    private UpdateType updateType;
    private LocalDate releaseDate;
    private LocalDateTime createDate;
    private LocalDateTime modifyDate;
    private User modifyUser;

    public Disc() {
    }

    public Disc(String asin, String title, DiscType discType, UpdateType updateType, boolean amazonLimit, LocalDate releaseDate) {
        this.asin = asin;
        this.title = title;
        this.discType = discType;
        this.updateType = updateType;
        this.amazonLimit = amazonLimit;
        this.releaseDate = releaseDate;
        this.createDate = LocalDateTime.now().withNano(0);
        this.modifyDate = LocalDateTime.now().withNano(0);
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

    @Column(length = 300)
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
    public Integer getTotalPt() {
        return totalPt;
    }

    public void setTotalPt(Integer totalPt) {
        this.totalPt = totalPt;
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
    public LocalDateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(LocalDateTime createDate) {
        this.createDate = createDate;
    }

    @Column
    public LocalDateTime getModifyDate() {
        return modifyDate;
    }

    public void setModifyDate(LocalDateTime modifyDate) {
        this.modifyDate = modifyDate;
    }

    @ManyToOne
    public User getModifyUser() {
        return modifyUser;
    }

    public void setModifyUser(User modifyUser) {
        this.modifyUser = modifyUser;
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

    private final DateTimeFormatter formatterTime = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private final DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("yyyy/MM/dd");

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
            object.put("thisRank", String.valueOf(getThisRank()));
        if (columns.contains("prevRank"))
            object.put("prevRank", String.valueOf(getPrevRank()));
        if (columns.contains("nicoBook"))
            object.put("nicoBook", String.valueOf(getNicoBook()));
        if (columns.contains("totalPt"))
            object.put("totalPt", String.valueOf(getTotalPt()));
        if (columns.contains("discType"))
            object.put("discType", getDiscType().name());
        if (columns.contains("updateType"))
            object.put("updateType", getUpdateType().name());
        if (columns.contains("releaseDate"))
            object.put("releaseDate", getReleaseDate().format(formatterDate));
        if (columns.contains("createDate"))
            object.put("createDate", getCreateDate().format(formatterTime));
        if (columns.contains("modifyDate"))
            object.put("modifyDate", Optional.ofNullable(getModifyDate())
                    .map(formatterTime::format).orElse("从未修改"));
        if (columns.contains("modifyUser"))
            object.put("modifyUser", Optional.ofNullable(getModifyUser())
                    .map(User::getUsername).orElse("无"));
        if (columns.contains("surplusDays"))
            object.put("surplusDays", getSurplusDays());
        return object;
    }

}
