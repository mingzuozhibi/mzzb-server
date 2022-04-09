package com.mingzuozhibi.commons.check;

import com.mingzuozhibi.commons.gson.GsonFactory;
import com.mingzuozhibi.commons.model.ErrorResult;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.Principal;
import java.util.Optional;

public abstract class CheckUtils {

    public static String paramBeExists(String paramName) {
        return errorResult(paramName + "已存在");
    }

    public static String paramNoExists(String paramName) {
        return errorResult(paramName + "不存在");
    }

    public static String doCreate(String entryName, String name, String json) {
        return String.format("[%s][创建=%s][name=%s][json=%s]", getLoginName(), entryName, name, json);
    }

    public static String doDelete(String entryName, String name, String json) {
        return String.format("[%s][删除=%s][name=%s][json=%s]", getLoginName(), entryName, name, json);
    }

    public static String doUpdate(String paramName, Object from, Object to) {
        return String.format("[%s][更新=%s][%s=>%s]", getLoginName(), paramName, from, to);
    }

    private static String getLoginName() {
        SecurityContext context = SecurityContextHolder.getContext();
        return Optional.ofNullable(context.getAuthentication())
            .map(Principal::getName)
            .orElse("System");
    }

    private static String errorResult(String message) {
        return GsonFactory.GSON.toJson(new ErrorResult(message));
    }

}
