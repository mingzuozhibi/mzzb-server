package com.mingzuozhibi.modules.disc;

import com.mingzuozhibi.commons.base.BaseModel2;
import com.mingzuozhibi.commons.gson.GsonIgnored;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class DiscGroup extends BaseModel2 implements Comparable<DiscGroup> {

    public enum ViewType {
        SakuraList, PublicList, PrivateList
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
    public int compareTo(DiscGroup discGroup) {
        Objects.requireNonNull(discGroup);
        return Comparator.comparing(DiscGroup::getKey).compare(this, discGroup);
    }

}
