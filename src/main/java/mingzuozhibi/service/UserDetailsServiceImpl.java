package mingzuozhibi.service;

import mingzuozhibi.persist.core.User;
import mingzuozhibi.persist.core.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@PropertySource("file:config/setting.properties")
public class UserDetailsServiceImpl implements UserDetailsService {

    private List<GrantedAuthority> ROLE_ADMIN;
    private List<GrantedAuthority> ROLE_USER;
    private HashSet<String> ADMIN_LIST;

    private UserRepository userRepository;

    @Value("${security.admin.password}")
    private String securityAdminPassword;

    @Value("${security.admin.userlist}")
    private String securityAdminUserlist;

    @Autowired
    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void initial() {
        setupConstants();
        setupAdminUser();
    }

    private void setupConstants() {
        ROLE_ADMIN = Stream.of("ROLE_BASIC", "ROLE_ADMIN")
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        ROLE_USER = Stream.of("ROLE_BASIC")
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        ADMIN_LIST = Stream.of(securityAdminUserlist.split(","))
                .collect(Collectors.toCollection(HashSet::new));
    }

    private void setupAdminUser() {
        Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

        User user = userRepository.findByUsername("admin");
        if (user == null) {
            user = new User();
            user.setUsername("admin");
            user.setPassword(securityAdminPassword);
            userRepository.save(user);

            logger.info("创建管理员用户");
        } else if (!securityAdminPassword.equals(user.getPassword())) {
            user.setPassword(securityAdminPassword);
            userRepository.save(user);

            logger.info("更新管理员密码");
        }
    }

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("username " + username + " not found");
        }
        return new UserDetailsImpl(user);
    }

    private class UserDetailsImpl implements UserDetails {

        private final User user;

        private UserDetailsImpl(User user) {
            this.user = user;
        }

        public List<GrantedAuthority> getAuthorities() {
            if (ADMIN_LIST.contains(user.getUsername())) {
                return ROLE_ADMIN;
            } else {
                return ROLE_USER;
            }
        }

        public String getPassword() {
            return user.getPassword();
        }

        public String getUsername() {
            return user.getUsername();
        }

        public boolean isEnabled() {
            return user.isEnabled();
        }

        public boolean isAccountNonLocked() {
            return true;
        }

        public boolean isAccountNonExpired() {
            return true;
        }

        public boolean isCredentialsNonExpired() {
            return true;
        }

    }

}
