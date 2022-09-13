package com.mingzuozhibi.modules.disc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;

import static com.mingzuozhibi.commons.gson.GsonFactory.GSON;
import static java.util.Comparator.*;

public abstract class DiscUtils {

    public static JsonObject buildWithCount(Group group, long count) {
        var object = GSON.toJsonTree(group).getAsJsonObject();
        object.addProperty("discCount", count);
        return object;
    }

    public static JsonObject buildWithDiscs(Group group) {
        var object = GSON.toJsonTree(group).getAsJsonObject();
        object.add("discs", buildDiscs(group.getDiscs()));
        return object;
    }

    private static JsonArray buildDiscs(Collection<Disc> discs) {
        var array = new JsonArray();
        discs.forEach(disc -> array.add(disc.toJson()));
        return array;
    }

    public static void updateRank(Disc disc, Integer rank, Instant instant) {
        disc.setPrevRank(disc.getThisRank());
        disc.setThisRank(rank);
        disc.setUpdateTime(instant);
        if (!Objects.equals(disc.getThisRank(), disc.getPrevRank())) {
            disc.setModifyTime(instant);
        }
    }

    public static Set<Disc> findNeedUpdate(List<Disc> discs, Instant needQuick, Instant needFetch) {
        Set<Disc> result = new LinkedHashSet<>();
        discs.stream()
            .filter(isNeedQuick(needQuick))
            .sorted(compareNeedQuick())
            .forEach(result::add);
        discs.stream()
            .filter(isNeedFetch(needFetch))
            .forEach(result::add);
        return result;
    }

    private static Comparator<Disc> compareNeedQuick() {
        return comparing(Disc::getUpdateTime, nullsLast(naturalOrder()));
    }

    private static Predicate<Disc> isNeedQuick(Instant needQuick) {
        return disc -> disc.getUpdateTime() == null ||
            disc.getUpdateTime().isBefore(needQuick);
    }

    private static Predicate<Disc> isNeedFetch(Instant needFetch) {
        return disc -> disc.getUpdateTime() != null &&
            disc.getUpdateTime().isBefore(needFetch);
    }

    public static Optional<Instant> findLastUpdate(Set<Disc> discs) {
        return discs.stream()
            .max(comparing(Disc::getUpdateTime, nullsFirst(naturalOrder())))
            .map(Disc::getUpdateTime);
    }

}
