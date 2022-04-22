package com.mingzuozhibi.modules.message;

import lombok.extern.slf4j.Slf4j;

@Slf4j
enum MessageType {

    info, notify, success, warning, danger;

    public boolean match(MessageType t) {
        return this.ordinal() <= t.ordinal();
    }

    public static MessageType parse(String type) {
        try {
            return MessageType.valueOf(type);
        } catch (IllegalArgumentException e) {
            log.warn("parse error: " + e);
            return MessageType.info;
        }
    }

}
