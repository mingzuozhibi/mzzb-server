package com.mingzuozhibi.modules.core;

import com.mingzuozhibi.commons.mylog.JmsEnums.Name;
import com.mingzuozhibi.commons.mylog.JmsEnums.Type;
import org.springframework.data.jpa.repository.*;

import java.util.Arrays;

public interface MessageRepository extends JpaRepository<Message, Long>, JpaSpecificationExecutor<Message> {

    default int cleanup(Name name, int size, Type... typeIn) {
        if (typeIn.length == 0) typeIn = Type.values();
        int[] types = Arrays.stream(typeIn).mapToInt(Enum::ordinal).toArray();
        Long id = findTargetId(name.name(), types, size * 20);
        if (id == null) return 0;
        return deleteByTargetId(name.name(), types, id);
    }

    @Query(value = "select id from message " +
        "where name = ?1 and type in ?2 " +
        "order by id desc limit ?3,1", nativeQuery = true)
    Long findTargetId(String name, int[] types, int size);

    @Modifying
    @Query(value = "delete from message where name = ?1 and type in ?2 and id <= ?3", nativeQuery = true)
    int deleteByTargetId(String name, int[] types, Long id);

}
