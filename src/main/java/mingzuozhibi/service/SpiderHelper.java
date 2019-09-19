package mingzuozhibi.service;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
public class SpiderHelper {

    public String gateway(String uri, Object... args) {
        return "http://localhost:9999" + String.format(uri, args);
    }

    public String waitRequest(String url) {
        return waitRequest(url, null);
    }

    public String waitRequest(String url, Consumer<Connection> consumer) {
        Exception lastThrown = null;
        for (int retry = 0; retry < 3; retry++) {
            try {
                Connection connection = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .timeout(10000);
                if (consumer != null) {
                    consumer.accept(connection);
                }
                return connection.execute().body();
            } catch (Exception e) {
                lastThrown = e;
            }
        }
        String format = "Jsoup: 无法获取网页内容[url=%s][message=%s]";
        String message = String.format(format, url, lastThrown.getMessage());
        throw new RuntimeException(message, lastThrown);
    }

}
