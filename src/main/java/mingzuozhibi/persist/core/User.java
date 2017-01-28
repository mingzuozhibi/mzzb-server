package mingzuozhibi.persist.core;

import com.fasterxml.jackson.annotation.*;
import mingzuozhibi.persist.BaseModel;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.util.Date;

@Entity
public class User extends BaseModel {

    private String password;
    private String username;
    private boolean enabled;

    private Date registerDate;
    private Date lastLoggedin;

    public User() {
        enabled = true;
        registerDate = new Date();
    }

    @JsonIgnore
    @Column(length = 40, nullable = false)
    public String getPassword() {
        return password;
    }

    @JsonProperty("password")
    public void setPassword(String password) {
        this.password = password;
    }

    @Column(length = 40, unique = true, nullable = false)
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Column
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public Date getRegisterDate() {
        return registerDate;
    }

    public void setRegisterDate(Date registerDate) {
        this.registerDate = registerDate;
    }

    @Column
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public Date getLastLoggedin() {
        return lastLoggedin;
    }

    public void setLastLoggedin(Date lastLoggedin) {
        this.lastLoggedin = lastLoggedin;
    }

}
