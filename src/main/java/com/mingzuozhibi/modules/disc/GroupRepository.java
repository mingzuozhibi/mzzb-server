package com.mingzuozhibi.modules.disc;

import com.mingzuozhibi.modules.disc.Group.ViewType;
import org.springframework.data.jpa.repository.*;

import javax.persistence.criteria.Predicate;
import java.util.*;
import java.util.stream.Collectors;

public interface GroupRepository extends JpaRepository<Group, Long>, JpaSpecificationExecutor<Group> {

    default List<Group> findBy(boolean hasPrivate, boolean hasDisable) {
        return findAll((root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (!hasPrivate) {
                predicates.add(cb.notEqual(root.get("viewType"), ViewType.PrivateList));
            }
            if (!hasDisable) {
                predicates.add(cb.notEqual(root.get("enabled"), false));
            }
            return query.where(predicates.toArray(new Predicate[0])).getRestriction();
        });
    }

    List<Group> findByEnabled(boolean enabled);

    Optional<Group> findByKey(String key);

    @Query(value = "Select g " +
        "From Group g " +
        "Join g.discs d " +
        "Where d.asin = :asin")
    List<Group> findByAsin(String asin);

    @Query(value = "select * from `group` " +
        "where enabled = true " +
        "order by `key` desc", nativeQuery = true)
    List<Group> findActiveDiscGroups();

    default List<Disc> findActiveDiscs() {
        return findActiveDiscGroups().stream()
            .flatMap(discGroup -> discGroup.getDiscs().stream())
            .collect(Collectors.toList());
    }

}
