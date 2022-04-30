package com.mingzuozhibi.commons.mylog;

public class JmsEnums {

    public static final String MODULE_MESSAGE = "module.message";
    public static final String MODULE_CONNECT = "module.connect";

    public static final String CONTENT_SEARCH = "content.search";
    public static final String CONTENT_RETURN = "content.return";

    public static final String NEED_UPDATE_ASINS = "need.update.asins";
    public static final String PREV_UPDATE_DISCS = "prev.update.discs";
    public static final String LAST_UPDATE_DISCS = "last.update.discs";

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
