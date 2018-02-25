package mingzuozhibi.security;

import mingzuozhibi.persist.User;
import mingzuozhibi.support.Dao;
import mingzuozhibi.support.PassUtil;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private Dao dao;
    private PassUtil passUtil;

    @Value("${mzzb.admin.password}")
    private String securityAdminPassword;

    @Autowired
    public UserDetailsServiceImpl(Dao dao, PassUtil passUtil) {
        this.dao = dao;
        this.passUtil = passUtil;
    }

    @PostConstruct
    public void initial() {
        setupAdminUser();
    }

    private void setupAdminUser() {
        Logger LOGGER = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

        String md5Password = passUtil.encode("admin", securityAdminPassword);

        dao.execute(session -> {
            User user = dao.lookup(User.class, "username", "admin");
            if (user == null) {
                user = new User("admin", md5Password);
                user.getRoles().add("ROLE_ADMIN");
                dao.save(user);

                LOGGER.info("创建管理员用户");
            }
            if (!passUtil.vaild(user, securityAdminPassword)) {
                user.setPassword(md5Password);

                LOGGER.info("更新管理员密码");
            }
            if (!user.getRoles().contains("ROLE_ADMIN")) {
                user.getRoles().add("ROLE_ADMIN");

                LOGGER.info("修复管理员权限");
            }
            if (!user.isEnabled()) {
                user.setEnabled(true);

                LOGGER.info("启用管理员权限");
            }
        });
    }

    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = dao.lookup(User.class, "username", username);
        if (user == null) {
            throw new UsernameNotFoundException("username " + username + " not found");
        }
        Hibernate.initialize(user.getRoles());
        return new UserDetailsImpl(user);
    }

}
