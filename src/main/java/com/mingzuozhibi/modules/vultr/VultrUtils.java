package com.mingzuozhibi.modules.vultr;

import com.mingzuozhibi.commons.logger.Logger;
import com.mingzuozhibi.commons.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import java.util.function.Consumer;

@Slf4j
public abstract class VultrUtils {

    private static String apiKey;
    private static Logger logger;

    public static void init(Logger logger, String apiKey) {
        VultrUtils.logger = logger;
        VultrUtils.apiKey = apiKey;
        var keylen = apiKey.length();
        var prefix = apiKey.substring(0, 2);
        var suffix = apiKey.substring(keylen - 2);
        log.debug("bcloud.apikey=%s**%s, length=%d".formatted(prefix, suffix, keylen));
    }

    public static Response jsoupPost(String url, String body) throws Exception {
        return jsoup(url, connection -> connection.method(Method.POST).requestBody(body));
    }

    public static String jsoupGet(String url) throws Exception {
        return jsoup(url, connection -> connection.method(Method.GET)).body();
    }

    public static Response jsoup(String url, Consumer<Connection> consumer) throws Exception {
        Exception lastThrow = null;
        int maxCount = 8;
        for (int i = 0; i < maxCount; i++) {
            try {
                Connection connection = Jsoup.connect(url)
                    .header("Authorization", "Bearer %s".formatted(apiKey))
                    .header("Content-Type", "application/json")
                    .timeout(10000)
                    .ignoreContentType(true);
                consumer.accept(connection);
                return connection.execute();
            } catch (Exception e) {
                lastThrow = e;
                logger.debug("jsoup(%s) throws %s (%d/%d)".formatted(url, e, i + 1, maxCount));
                ThreadUtils.sleepSeconds(3, 5);
            }
        }
        throw lastThrow;
    }

}
