package com.mingzuozhibi.modules.remember;

import com.mingzuozhibi.commons.base.BaseEntity;
import com.mingzuozhibi.modules.user.User;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "auto_login")
public class Remember extends BaseEntity implements Serializable {

    public Remember(User user, String token, Instant expired) {
        this.user = user;
        this.token = token;
        this.expired = expired;
    }

    @ManyToOne(optional = false)
    private User user;

    @Column(length = 36, nullable = false)
    private String token;

    @Column(nullable = false)
    private Instant expired;

}
