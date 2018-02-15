package mingzuozhibi.persist;

import org.json.JSONObject;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Entity
public class User extends BaseModel implements Serializable {

    private String username;
    private String password;
    private boolean enabled;
    private Set<String> roles = new HashSet<>();

    private LocalDateTime registerDate;
    private LocalDateTime lastLoggedIn;

    public User() {
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.enabled = true;
        this.registerDate = LocalDateTime.now().withNano(0);
        this.roles.add("ROLE_BASIC");
    }

    @Column(length = 50, nullable = false)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Column(length = 50, unique = true, nullable = false)
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Column(nullable = false)
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @ElementCollection
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    @Column(nullable = false)
    public LocalDateTime getRegisterDate() {
        return registerDate;
    }

    public void setRegisterDate(LocalDateTime registerDate) {
        this.registerDate = registerDate;
    }

    @Column
    public LocalDateTime getLastLoggedIn() {
        return lastLoggedIn;
    }

    public void setLastLoggedIn(LocalDateTime lastLoggedIn) {
        this.lastLoggedIn = lastLoggedIn;
    }

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

    private static final DateTimeFormatter formatterTime = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    public JSONObject toJSON() {
        JSONObject object = new JSONObject();
        object.put("id", getId());
        object.put("username", getUsername());
        object.put("enabled", enabled);
        object.put("registerDate", registerDate.format(formatterTime));
        String loginTime = Optional.ofNullable(lastLoggedIn)
                .map(formatterTime::format).orElse("从未登录");
        object.put("lastLoggedIn", loginTime);
        return object;
    }

}
