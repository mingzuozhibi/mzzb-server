package com.mingzuozhibi.support;

import java.security.Principal;

import static com.mingzuozhibi.modules.user.SessionUtils.getAuthentication;

public abstract class ModifyUtils {

    public static String getName() {
        return getAuthentication().map(Principal::getName).orElse("*system*");
    }

    public static String logCreate(String entryName, String name, String json) {
        return "[%s][创建%s][name=%s][json=%s]".formatted(getName(), entryName, name, json);
    }

    public static String logUpdate(String paramName, Object from, Object to, String name) {
        return "[%s][更新%s][%s=>%s][name=%s]".formatted(getName(), paramName, from, to, name);
    }

    public static String logDelete(String entryName, String name, String json) {
        return "[%s][删除%s][name=%s][json=%s]".formatted(getName(), entryName, name, json);
    }

    public static String logPush(String paramName, String itemName, String listName) {
        return "[%s][添加%s到列表][item=%s][list=%s]".formatted(getName(), paramName, itemName, listName);
    }

    public static String logDrop(String paramName, String itemName, String listName) {
        return "[%s][移除%s从列表][item=%s][list=%s]".formatted(getName(), paramName, itemName, listName);
    }

}
