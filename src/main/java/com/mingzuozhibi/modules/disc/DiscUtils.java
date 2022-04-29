package com.mingzuozhibi.modules.disc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Collection;

import static com.mingzuozhibi.commons.gson.GsonFactory.GSON;

public abstract class DiscUtils {

    public static JsonObject buildWithCount(Group group, long count) {
        JsonObject object = GSON.toJsonTree(group).getAsJsonObject();
        object.addProperty("discCount", count);
        return object;
    }

    public static JsonObject buildWithDiscs(Group group) {
        JsonObject object = GSON.toJsonTree(group).getAsJsonObject();
        object.add("discs", buildDiscs(group.getDiscs()));
        return object;
    }

    private static JsonArray buildDiscs(Collection<Disc> discs) {
        JsonArray array = new JsonArray();
        discs.forEach(disc -> array.add(disc.toJson()));
        return array;
    }

}
