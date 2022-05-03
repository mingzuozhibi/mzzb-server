package com.mingzuozhibi.commons.mylog;

public class JmsEnums {

    public static final String MODULE_MESSAGE = "module.message";
    public static final String CONTENT_SEARCH = "content.search";
    public static final String CONTENT_RETURN = "content.return";
    public static final String HISTORY_UPDATE = "history.update";
    public static final String HISTORY_FINISH = "history.finish";

    public static final String NEED_UPDATE_ASINS = "need.update.asins";
    public static final String NEXT_UPDATE_ASINS = "next.update.asins";

    public static final String DONE_UPDATE_DISCS = "done.update.discs";
    public static final String PREV_UPDATE_DISCS = "prev.update.discs";
    public static final String LAST_UPDATE_DISCS = "last.update.discs";
    public static final String RECORDS_ASIN_RANK = "records.asin.rank";

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
