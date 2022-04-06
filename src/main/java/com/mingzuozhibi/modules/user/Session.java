package com.mingzuozhibi.modules.user;

import com.mingzuozhibi.commons.BaseModel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity(name = "auto_login")
public class Session extends BaseModel implements Serializable {

    private User user;
    private String token;
    private LocalDateTime expired;

    public Session() {
    }

    public Session(User user, String token, LocalDateTime expired) {
        this.user = user;
        this.token = token;
        this.expired = expired;
    }

    @ManyToOne(optional = false)
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Column(length = 36, nullable = false)
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Column(nullable = false)
    public LocalDateTime getExpired() {
        return expired;
    }

    public void setExpired(LocalDateTime expired) {
        this.expired = expired;
    }

}
