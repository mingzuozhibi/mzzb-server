package com.mingzuozhibi.modules.user;

import com.mingzuozhibi.commons.BaseModel2;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "auto_login")
public class Session extends BaseModel2 implements Serializable {

    public Session(User user, String token, Instant expired) {
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
