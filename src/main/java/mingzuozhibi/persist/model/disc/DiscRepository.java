package mingzuozhibi.persist.model.disc;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscRepository extends PagingAndSortingRepository<Disc, Long> {

    Disc findByAsin(@Param("asin") String asin);

}
