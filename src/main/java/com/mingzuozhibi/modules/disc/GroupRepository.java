package com.mingzuozhibi.modules.disc;

import com.mingzuozhibi.modules.disc.Group.ViewType;
import org.springframework.data.jpa.repository.*;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.util.*;

@Transactional
public interface GroupRepository extends JpaRepository<Group, Long>, JpaSpecificationExecutor<Group> {

    Optional<Group> findByKey(String key);

    List<Group> findByEnabled(boolean enabled);

    @Query(value = "Select g " +
        "From Group g " +
        "Join g.discs d " +
        "Where d.asin = :asin")
    List<Group> findByAsin(String asin);

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

    default void updateModifyTime() {
        findAll().forEach(group -> {
            if (group.isEnabled()) {
                var lastUpdateOpt = DiscUtils.findLastUpdate(group.getDiscs());
                group.setModifyTime(lastUpdateOpt.orElse(null));
            } else {
                group.setModifyTime(null);
            }
        });
    }

}
