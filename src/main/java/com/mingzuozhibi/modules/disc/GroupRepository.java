package com.mingzuozhibi.modules.disc;

import com.mingzuozhibi.modules.disc.Group.ViewType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Transactional
public interface GroupRepository extends JpaRepository<Group, Long>, JpaSpecificationExecutor<Group> {

    Optional<Group> findByKey(String key);

    List<Group> findByEnabled(boolean enabled);

    List<Group> findByViewType(ViewType viewType);

    List<Group> findByEnabledAndViewType(boolean enabled, ViewType viewType, Pageable pageable);

    @Query(value = "Select g " +
        "From Group g " +
        "Join g.discs d " +
        "Where d.asin = :asin")
    List<Group> findByAsin(String asin);

    default List<Group> findByFilter(String filter) {
        var groups = new LinkedList<Group>();
        switch (filter) {
            case "top" -> {
                var pageRequest = PageRequest.of(0, 6, Sort.by(Sort.Order.desc("key")));
                groups.addAll(findByEnabled(true));
                groups.addAll(findByEnabledAndViewType(false, ViewType.PublicList, pageRequest));
            }
            case "pub" -> groups.addAll(findByViewType(ViewType.PublicList));
            case "all" -> groups.addAll(findAll());
        }
        return groups;
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

    @Query(value = "Select g.key " +
        "From Group g " +
        "Order By g.id Desc")
    List<String> listKeys();

    @Query(value = "Select g.title " +
        "From Group g " +
        "Order By g.id Desc")
    List<String> listTitles();

}
