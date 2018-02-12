package mingzuozhibi.persist;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Entity
public class Sakura extends BaseModel implements Comparable<Sakura> {

    public static final String TOP100 = "9999-99";

    private String key;
    private String title;
    private boolean enabled;
    private LocalDateTime sakuraUpdateDate;
    private List<Disc> discs = new LinkedList<>();

    public Sakura() {
    }

    public Sakura(String key, String title) {
        if (key.isEmpty()) {
            this.key = TOP100;
            this.title = "日亚实时TOP100";
        } else {
            this.key = key;
            this.title = Optional.ofNullable(title).orElseGet(() -> titleOfKey(key));
        }
        enabled = true;
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

    @Column
    public LocalDateTime getSakuraUpdateDate() {
        return sakuraUpdateDate;
    }

    public void setSakuraUpdateDate(LocalDateTime sakuraUpdateDate) {
        this.sakuraUpdateDate = sakuraUpdateDate;
    }

    @ManyToMany
    @JoinTable(name = "sakura_discs",
            joinColumns = {@JoinColumn(name = "sakura_id")},
            inverseJoinColumns = {@JoinColumn(name = "disc_id")})
    public List<Disc> getDiscs() {
        return discs;
    }

    public void setDiscs(List<Disc> discs) {
        this.discs = discs;
    }

    @Transient
    public boolean isTop100() {
        return TOP100.equals(getKey());
    }

    @Transient
    public static String titleOfKey(String key) {
        return key.substring(0, 4) + "年" + key.substring(5) + "月新番";
    }

    @Override
    public int compareTo(Sakura o) {
        Objects.requireNonNull(o);
        return Comparator.comparing(Sakura::getKey).compare(this, o);
    }

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    public JSONObject toJSON() {
        JSONObject object = new JSONObject();
        object.put("id", getId());
        object.put("key", getKey());
        object.put("title", getTitle());
        object.put("enabled", isEnabled());
        object.put("sakuraUpdateDate", Optional.ofNullable(sakuraUpdateDate)
                .map(date -> date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                .orElse(0L));
        return object;
    }

}
