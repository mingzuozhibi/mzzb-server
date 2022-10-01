package com.mingzuozhibi.modules.core;

import com.mingzuozhibi.commons.base.BaseKeys;
import com.mingzuozhibi.commons.base.BaseKeys.Name;
import com.mingzuozhibi.commons.base.BaseKeys.Type;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.*;

@Transactional
public interface MessageRepository extends JpaRepository<Message, Long>, JpaSpecificationExecutor<Message> {

    default Page<Message> findBy(Name name, List<Type> types, String search,
                                 Instant start, Instant end, Pageable pageable) {
        return findAll((Specification<Message>) (root, query, cb) -> {
            var array = new ArrayList<Predicate>();
            array.add(cb.equal(root.get("name"), name));
            if (types != null && !types.isEmpty() && types.size() < BaseKeys.Type.values().length) {
                array.add(cb.in(root.get("type")).value(types));
            }
            if (StringUtils.isNotBlank(search)) {
                array.add(cb.like(root.get("text"), "%" + search + "%"));
            }
            if (start != null) {
                array.add(cb.greaterThan(root.get("createOn"), start));
            }
            if (end != null) {
                array.add(cb.lessThan(root.get("createOn"), end));
            }
            return query.where(array.toArray(new Predicate[0])).getRestriction();
        }, pageable);
    }

    default int cleanup(Name name, int page, Type... typeIn) {
        if (typeIn.length == 0) typeIn = Type.values();
        var types = Arrays.stream(typeIn).mapToInt(Enum::ordinal).toArray();
        var id = findTargetId(name.name(), page * 20);
        if (id == null) return 0;
        return deleteByTargetId(name.name(), types, id);
    }

    @Query(value = "select id from message" +
        " where name = ?1" +
        " order by id desc" +
        " limit ?2,1", nativeQuery = true)
    Long findTargetId(String name, int size);

    @Modifying
    @Query(value = "delete from message" +
        " where name = ?1" +
        " and type in ?2" +
        " and id <= ?3", nativeQuery = true)
    int deleteByTargetId(String name, int[] types, Long id);

}
