package mingzuozhibi.persist.disc;

import mingzuozhibi.persist.BaseModel;
import org.json.JSONObject;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Entity
public class DiscGroup extends BaseModel implements Comparable<DiscGroup> {

    public enum ViewType {
        SakuraList, PublicList, PrivateList
    }

    private String key;
    private String title;
    private boolean enabled;
    private ViewType viewType;
    private LocalDateTime modifyTime;
    private Set<Disc> discs = new HashSet<>();

    public DiscGroup() {
    }

    public DiscGroup(String key, String title, boolean enabled, ViewType viewType) {
        this.key = key;
        this.title = title == null ? titleOfKey(key) : title;
        this.enabled = enabled;
        this.viewType = viewType;
    }

    @Column(name = "`key`", length = 100, nullable = false, unique = true)
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Column(length = 100, nullable = false)
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Column(nullable = false)
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Column(nullable = false)
    public ViewType getViewType() {
        return viewType;
    }

    public void setViewType(ViewType viewType) {
        this.viewType = viewType;
    }

    @Column
    public LocalDateTime getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(LocalDateTime modifyTime) {
        this.modifyTime = modifyTime;
    }

    @ManyToMany
    @JoinTable(name = "disc_group_discs",
        joinColumns = {@JoinColumn(name = "disc_group_id")},
        inverseJoinColumns = {@JoinColumn(name = "disc_id")})
    public Set<Disc> getDiscs() {
        return discs;
    }

    public void setDiscs(Set<Disc> discs) {
        this.discs = discs;
    }

    @Override
    public int compareTo(DiscGroup o) {
        Objects.requireNonNull(o);
        return Comparator.comparing(DiscGroup::getKey).compare(this, o);
    }

    public JSONObject toJSON() {
        JSONObject object = new JSONObject();
        object.put("id", getId());
        object.put("key", getKey());
        object.put("title", getTitle());
        object.put("enabled", isEnabled());
        object.put("viewType", getViewType());
        if (isEnabled() && getModifyTime() != null) {
            object.put("modifyTime", toEpochMilli(modifyTime));
        }
        return object;
    }

    private static String titleOfKey(String key) {
        Objects.requireNonNull(key);
        if (key.equals("9999-99")) {
            return "日亚实时TOP100";
        }
        Matcher matcher = Pattern.compile("^(\\d{4})-(\\d{2})$").matcher(key);
        if (matcher.find()) {
            return String.format("%s年%s月新番", matcher.group(1), matcher.group(2));
        }
        return "未命名列表";
    }

}
