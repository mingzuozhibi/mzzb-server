package mingzuozhibi.persist.model.discList;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscListRepository extends PagingAndSortingRepository<DiscList, Long> {

    DiscList findByName(@Param("name") String name);

    List<DiscList> findBySakura(@Param("sakura") boolean sakura);

}
