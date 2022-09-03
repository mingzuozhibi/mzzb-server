package com.mingzuozhibi.commons.base;

public abstract class BaseKeys {

    public static final String FETCH_TASK_START = "fetch.task.start";
    public static final String FETCH_TASK_DONE1 = "fetch.task.done1";
    public static final String FETCH_TASK_DONE2 = "fetch.task.done2";

    public static final String HISTORY_FINISH = "history.finish";
    public static final String CONTENT_FINISH = "content.finish";

    public static final String MODULE_MESSAGE = "module.message";
    public static final String CONTENT_SEARCH = "content.search";
    public static final String CONTENT_RETURN = "content.return";

    public enum Type {
        DEBUG,
        INFO,
        NOTIFY,
        SUCCESS,
        WARNING,
        ERROR,
    }

    public enum Name {
        SPIDER_CONTENT,
        SPIDER_HISTORY,
        SERVER_DISC,
        SERVER_USER,
        SERVER_CORE,
        DEFAULT,
    }

}
