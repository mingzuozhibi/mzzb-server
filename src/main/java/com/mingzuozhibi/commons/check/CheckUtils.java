package com.mingzuozhibi.commons.check;

import org.json.JSONObject;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.Principal;
import java.util.Optional;

public abstract class CheckUtils {

    public static String paramBeExists(String paramName) {
        return errorMessage(paramName + "已存在");
    }

    public static String paramNoExists(String paramName) {
        return errorMessage(paramName + "不存在");
    }

    public static String doCreate(String entryName, String name, JSONObject json) {
        return String.format("[%s][创建%s][name=%s][json=%s]", getLoginName(), entryName, name, json);
    }

    public static String doUpdate(String paramName, Object from, Object to) {
        return String.format("[%s][更新%s][%s=>%s]", getLoginName(), paramName, from, to);
    }

    private static String getLoginName() {
        SecurityContext context = SecurityContextHolder.getContext();
        return Optional.ofNullable(context.getAuthentication())
            .map(Principal::getName)
            .orElse("Unknown");
    }

    private static String errorMessage(String message) {
        JSONObject root = new JSONObject();
        root.put("success", false);
        root.put("message", message);
        return root.toString();
    }

}
