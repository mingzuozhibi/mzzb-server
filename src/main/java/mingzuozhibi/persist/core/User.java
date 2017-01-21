package mingzuozhibi.persist.core;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import mingzuozhibi.persist.BaseModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
public class User extends BaseModel implements UserDetails {

    private String password;
    private String username;
    private String userRoles;

    private boolean enabled;
    private boolean accountNonLocked;
    private boolean accountNonExpired;
    private boolean credentialsNonExpired;

    private Date registerDate;
    private Date lastLoggedin;

    public User() {
        userRoles = "ROLE_BASIC";
        enabled = true;
        accountNonLocked = true;
        accountNonExpired = true;
        credentialsNonExpired = true;
        registerDate = new Date();
    }

    @JsonIgnore
    @Column(length = 40, nullable = false)
    public String getPassword() {
        return password;
    }

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

    @Column(length = 100)
    public String getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(String userRoles) {
        this.userRoles = userRoles;
    }

    @Column
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Column
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    public void setAccountNonLocked(boolean accountNonLocked) {
        this.accountNonLocked = accountNonLocked;
    }

    @Column
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    public void setAccountNonExpired(boolean accountNonExpired) {
        this.accountNonExpired = accountNonExpired;
    }

    @Column
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    public void setCredentialsNonExpired(boolean credentialsNonExpired) {
        this.credentialsNonExpired = credentialsNonExpired;
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

    @Transient
    @JsonIgnore
    public List<GrantedAuthority> getAuthorities() {
        Logger logger = LoggerFactory.getLogger(this.getClass());
        logger.debug("检查访问权限: 用户: %s, 权限: %s ", username, userRoles);

        if (userRoles == null) {
            return new ArrayList<>(0);
        }

        return Arrays.stream(userRoles.split(","))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

}
