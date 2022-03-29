package com.mingzuozhibi.jms;

public interface JmsMessage {

    void info(String format, Object... args);

    void success(String format, Object... args);

    void notify(String format, Object... args);

    void warning(String format, Object... args);

    void danger(String format, Object... args);

    void info(String message);

    void success(String message);

    void notify(String message);

    void warning(String message);

    void danger(String message);

    void sendMsgNoLog(String type, String message);

    void infoAndSend(String type, String message);

    void infoAndSend(String type, String message, String infoLog);

}
