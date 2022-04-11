package com.mingzuozhibi.commons.utils;

import static com.mingzuozhibi.commons.utils.SessionUtils.getLoginName;

public abstract class ModifyUtils {

    public static String logCreate(String entryName, String name, String json) {
        return String.format("[%s][创建%s][name=%s][json=%s]", getLoginName(), entryName, name, json);
    }

    public static String logDelete(String entryName, String name, String json) {
        return String.format("[%s][删除%s][name=%s][json=%s]", getLoginName(), entryName, name, json);
    }

    public static String logUpdate(String paramName, Object from, Object to) {
        return String.format("[%s][更新%s][%s=>%s]", getLoginName(), paramName, from, to);
    }

    public static String logUpdate(String paramName, Object from, Object to, String name) {
        return String.format("[%s][更新%s][%s=>%s][name=%s]", getLoginName(), paramName, from, to, name);
    }

    public static String logPush(String paramName, String itemName, String listName) {
        return String.format("[%s][添加%s到列表][项目=%s][列表=%s]", getLoginName(), paramName, itemName, listName);
    }

    public static String logDrop(String paramName, String itemName, String listName) {
        return String.format("[%s][移除%s从列表][项目=%s][列表=%s]", getLoginName(), paramName, itemName, listName);
    }

}
