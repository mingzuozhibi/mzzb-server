package com.mingzuozhibi.modules.group;

import com.mingzuozhibi.commons.BaseModel2;
import com.mingzuozhibi.commons.gson.GsonIgnored;
import com.mingzuozhibi.modules.disc.Disc;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.json.JSONObject;

import javax.persistence.*;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class DiscGroup extends BaseModel2 implements Comparable<DiscGroup> {

    public enum ViewType {
        SakuraList, PublicList, PrivateList
    }

    public DiscGroup(String key, String title, boolean enabled, ViewType viewType) {
        this.key = key;
        this.title = title == null ? titleOfKey(key) : title;
        this.enabled = enabled;
        this.viewType = viewType;
    }

    @Column(name = "`key`", length = 100, nullable = false, unique = true)
    private String key;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private ViewType viewType;

    @Column
    private Instant modifyTime;

    @GsonIgnored
    @ManyToMany
    @JoinTable(name = "disc_group_discs",
        joinColumns = {@JoinColumn(name = "disc_group_id")},
        inverseJoinColumns = {@JoinColumn(name = "disc_id")})
    private Set<Disc> discs = new HashSet<>();

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
            object.put("modifyTime", modifyTime.toEpochMilli());
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
