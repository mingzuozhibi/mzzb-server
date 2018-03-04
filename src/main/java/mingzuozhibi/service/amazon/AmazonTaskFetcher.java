package mingzuozhibi.service.amazon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class AmazonTaskFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmazonTaskFetcher.class);
    private DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    private SignedRequestsHelper requestsHelper;

    private LocalDateTime createTime;
    private long connectTime;
    private long totalTaskCount;
    private long errorTaskCount;
    private long totalConnectCount;
    private long errorConnectCount;
    private long er503ConnectCount;
    private long er400ConnectCount;
    private long erdocConnectCount;

    public AmazonTaskFetcher(String accessKey, String secretKey, String associateTag) {
        initRequestHelper(accessKey, secretKey, associateTag);
        this.createTime = LocalDateTime.now().withNano(0);
    }

    private void initRequestHelper(String accessKey, String secretKey, String associateTag) {
        String endpoint = "ecs.amazonaws.jp";
        try {
            requestsHelper = SignedRequestsHelper.getInstance(endpoint, accessKey, secretKey, associateTag);
        } catch (Exception e) {
            LOGGER.warn("创建SignedRequestsHelper失败，associateTag={}", associateTag);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("创建SignedRequestsHelper失败", e);
            }
        }
    }

    public void fetch(AmazonTask task, Consumer<AmazonTask> consumer) {
        Instant start = Instant.now();
        long totalConnectCount = this.totalConnectCount;
        long er503ConnectCount = this.er503ConnectCount;
        long er400ConnectCount = this.er400ConnectCount;
        long erdocConnectCount = this.erdocConnectCount;
        long errorConnectCount = this.errorConnectCount;

        run(task, consumer);

        long cost = Instant.now().toEpochMilli() - start.toEpochMilli();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[Amazon更新器][done={}][asin={}][cost={}ms][total={}][er503={}][er400={}][erdoc={}][error={}]",
                    task.isDone(), task.getAsin(), cost,
                    this.totalConnectCount - totalConnectCount,
                    this.er503ConnectCount - er503ConnectCount,
                    this.er400ConnectCount - er400ConnectCount,
                    this.erdocConnectCount - erdocConnectCount,
                    this.errorConnectCount - errorConnectCount);
        }

        this.connectTime += cost;
    }

    private void run(AmazonTask task, Consumer<AmazonTask> consumer) {
        totalTaskCount++;
        String requestUrl = getRequestUrl(task);

        LOOP:
        while (task.getErrorCount() <= task.getMaxRetryCount()) {
            try {
                totalConnectCount++;
                task.setDocument(getDocument(requestUrl));
                task.setDone(true);
                task.setFinishTime(LocalDateTime.now().withNano(0));
                consumer.accept(task);
                return;
            } catch (IOException e) {
                switch (e.getMessage()) {
                    case "Server returned HTTP response code: 503":
                        er503ConnectCount++;
                        threadSleep(1000);
                        break;
                    case "Server returned HTTP response code: 400":
                        er400ConnectCount++;
                        task.setErrorCount(task.getErrorCount() + 1);
                        task.setErrorMessage(e.getMessage());
                        break;
                    default:
                        errorConnectCount++;
                        task.setErrorCount(task.getErrorCount() + 1);
                        task.setErrorMessage(e.getMessage());
                        break;
                }
            } catch (ParserConfigurationException | SAXException e) {
                erdocConnectCount++;
                task.setErrorCount(task.getErrorCount() + 1);
                task.setErrorMessage(e.getMessage());
            }
        }
        errorTaskCount++;
    }

    private Document getDocument(String requestUrl) throws IOException, SAXException, ParserConfigurationException {
        InputStream inputStream = new URL(requestUrl).openStream();
        return dbf.newDocumentBuilder().parse(inputStream);
    }

    private void threadSleep(int timeout) {
        try {
            TimeUnit.MILLISECONDS.sleep(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private String getRequestUrl(AmazonTask task) {
        Map<String, String> params = new HashMap<>();
        params.put("Service", "AWSECommerceService");
        params.put("Version", "2013-08-01");
        params.put("Operation", "ItemLookup");
        params.put("ItemId", task.getAsin());
        params.put("ResponseGroup", task.getParam());
        return requestsHelper.sign(params);
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public long getConnectTime() {
        return connectTime;
    }

    public long getTotalTaskCount() {
        return totalTaskCount;
    }

    public long getErrorTaskCount() {
        return errorTaskCount;
    }

    public long getTotalConnectCount() {
        return totalConnectCount;
    }

    public long getErrorConnectCount() {
        return errorConnectCount;
    }

    public long getEr503ConnectCount() {
        return er503ConnectCount;
    }

}
