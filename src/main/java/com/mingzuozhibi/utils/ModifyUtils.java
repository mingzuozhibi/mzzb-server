package com.mingzuozhibi.utils;

import java.security.Principal;

import static com.mingzuozhibi.modules.session.SessionUtils.getAuthentication;

public abstract class ModifyUtils {

    private static String getName() {
        return getAuthentication().map(Principal::getName).orElse("*system*");
    }

    public static String logCreate(String entryName, String name, String json) {
        return String.format("[%s][创建%s][name=%s][json=%s]", getName(), entryName, name, json);
    }

    public static String logDelete(String entryName, String name, String json) {
        return String.format("[%s][删除%s][name=%s][json=%s]", getName(), entryName, name, json);
    }

    public static String logUpdate(String paramName, Object from, Object to) {
        return String.format("[%s][更新%s][%s=>%s]", getName(), paramName, from, to);
    }

    public static String logUpdate(String paramName, Object from, Object to, String name) {
        return String.format("[%s][更新%s][%s=>%s][name=%s]", getName(), paramName, from, to, name);
    }

    public static String logPush(String paramName, String itemName, String listName) {
        return String.format("[%s][添加%s到列表][项目=%s][列表=%s]", getName(), paramName, itemName, listName);
    }

    public static String logDrop(String paramName, String itemName, String listName) {
        return String.format("[%s][移除%s从列表][项目=%s][列表=%s]", getName(), paramName, itemName, listName);
    }

}
