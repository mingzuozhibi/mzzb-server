package com.mingzuozhibi.modules.user;

import com.mingzuozhibi.commons.BaseModel2;
import com.mingzuozhibi.commons.gson.GsonIgnored;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseModel2 implements Serializable {

    public User(String username, String password, boolean enabled) {
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.registerDate = Instant.now();
        this.roles.add("ROLE_BASIC");
    }

    @Column(length = 32, unique = true, nullable = false)
    private String username;

    @GsonIgnored
    @Column(length = 32, nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean enabled;

    @ElementCollection
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    private Set<String> roles = new HashSet<>();

    @Column(nullable = false)
    public Instant registerDate;

    @Column
    public Instant lastLoggedIn;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

}
