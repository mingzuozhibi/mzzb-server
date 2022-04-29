package com.mingzuozhibi.modules.disc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mingzuozhibi.commons.base.BaseEntity;
import lombok.*;

import javax.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "disc_group")
public class Group extends BaseEntity {

    private static final long serialVersionUID = 100L;

    public enum ViewType {
        SakuraList, PublicList, PrivateList
    }

    public Group(String key, String title, Boolean enabled, ViewType viewType) {
        this.key = key;
        this.title = title;
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

    @JsonIgnore
    @ManyToMany
    @JoinTable(name = "disc_group_discs",
        joinColumns = {@JoinColumn(name = "disc_group_id")},
        inverseJoinColumns = {@JoinColumn(name = "disc_id")})
    private Set<Disc> discs = new HashSet<>();

}
