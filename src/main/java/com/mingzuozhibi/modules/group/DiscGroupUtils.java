package com.mingzuozhibi.modules.group;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mingzuozhibi.modules.disc.Disc;

import java.util.Collection;

import static com.mingzuozhibi.commons.gson.GsonFactory.GSON;

public abstract class DiscGroupUtils {

    public static JsonObject buildWithCount(DiscGroup discGroup, long count) {
        JsonObject object = GSON.toJsonTree(discGroup).getAsJsonObject();
        object.addProperty("discCount", count);
        return object;
    }

    public static JsonObject buildWithDiscs(DiscGroup discGroup) {
        JsonObject object = GSON.toJsonTree(discGroup).getAsJsonObject();
        object.add("discs", buildDiscs(discGroup.getDiscs()));
        return object;
    }

    private static JsonArray buildDiscs(Collection<Disc> discs) {
        JsonArray array = new JsonArray();
        discs.forEach(disc -> array.add(disc.toJson()));
        return array;
    }

}
