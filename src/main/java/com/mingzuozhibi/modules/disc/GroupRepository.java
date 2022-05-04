package com.mingzuozhibi.modules.disc;

import com.mingzuozhibi.modules.disc.Group.ViewType;
import org.springframework.data.jpa.repository.*;

import javax.persistence.criteria.Predicate;
import java.util.*;

public interface GroupRepository extends JpaRepository<Group, Long>, JpaSpecificationExecutor<Group> {

    default List<Group> findBy(boolean hasPrivate, boolean hasDisable) {
        return findAll((root, query, cb) -> {
            ArrayList<Predicate> predicates = new ArrayList<>();
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

    @Query(value = "select * from disc_group where enabled = true order by `key` desc", nativeQuery = true)
    List<Group> findActiveDiscGroups();

}
