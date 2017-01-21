package mingzuozhibi.persist.core;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDao extends PagingAndSortingRepository<User, Long> {

    User findByUsername(@Param("username") String username);

}
