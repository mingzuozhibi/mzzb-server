package com.mingzuozhibi.modules.user;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class UserUtils {

    public static JSONArray buildUsers(List<User> users) {
        JSONArray result = new JSONArray();
        users.forEach(user -> result.put(user.toJSON()));
        return result;
    }

    public static JSONObject buildUser(User user) {
        return user.toJSON();
    }

}
