package com.mingzuozhibi.commons.utils;

public abstract class ModifyUtils {

    public static String doCreate(String entryName, String name, String json) {
        return String.format("[%s][创建=%s][name=%s][json=%s]", SecurityUtils.loginName(), entryName, name, json);
    }

    public static String doDelete(String entryName, String name, String json) {
        return String.format("[%s][删除=%s][name=%s][json=%s]", SecurityUtils.loginName(), entryName, name, json);
    }

    public static String doUpdate(String paramName, Object from, Object to) {
        return String.format("[%s][更新=%s][%s=>%s]", SecurityUtils.loginName(), paramName, from, to);
    }

    public static String doPush(String paramName, Object from, Object to) {
        return String.format("[%s][添加=%s][%s=>%s]", SecurityUtils.loginName(), paramName, from, to);
    }

    public static String doDrop(String paramName, Object from, Object to) {
        return String.format("[%s][移除=%s][%s=>%s]", SecurityUtils.loginName(), paramName, from, to);
    }

}
