package com.mingzuozhibi.modules.disc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.time.Instant;
import java.util.Collection;
import java.util.Objects;

import static com.mingzuozhibi.commons.gson.GsonFactory.GSON;

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

}
