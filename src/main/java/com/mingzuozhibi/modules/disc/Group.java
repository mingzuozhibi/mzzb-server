package com.mingzuozhibi.modules.disc;

import com.mingzuozhibi.commons.base.BaseEntity;
import com.mingzuozhibi.commons.gson.GsonIgnored;
import lombok.*;

import javax.persistence.*;
import java.io.Serial;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Group extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 100L;

    @SuppressWarnings("unused")
    public enum ViewType {
        SakuraList, PublicList, PrivateList
    }

    public Group(String key, String title, Boolean enabled, ViewType viewType) {
        this.key = key;
        this.title = title;
        this.enabled = enabled;
        this.viewType = viewType;
    }

    @Column(length = 100, nullable = false, unique = true)
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
    @JoinTable(name = "group_discs",
        joinColumns = {@JoinColumn(name = "group_id")},
        inverseJoinColumns = {@JoinColumn(name = "disc_id")})
    private Set<Disc> discs = new HashSet<>();

}
