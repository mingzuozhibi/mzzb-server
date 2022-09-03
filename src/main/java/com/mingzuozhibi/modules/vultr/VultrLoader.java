package com.mingzuozhibi.modules.vultr;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

public abstract class VultrLoader {

    @Getter
    @Setter
    public static class Region {
        private String code;
        private String city;
        private String nation;
        private String area;
    }

    public static List<Region> loadRegions() {
        var loader = VultrLoader.class.getClassLoader();
        try (var in = Objects.requireNonNull(loader.getResourceAsStream("regions.json"));
             var br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            while (br.ready()) {
                sb.append(br.readLine());
            }
            var gson = new GsonBuilder().create();
            var token = TypeToken.getParameterized(List.class, Region.class);
            return gson.fromJson(sb.toString(), token.getType());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
