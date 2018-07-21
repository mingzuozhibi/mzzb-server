package mingzuozhibi.service.amazon;

import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.SocketTimeoutException;
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
    private String associateTag;

    private LocalDateTime createTime;
    private long connectTime;
    private long totalTaskCount;
    private long errorTaskCount;
    private long totalConnectCount;
    private long errorConnectCount;
    private long er503ConnectCount;
    private long er400ConnectCount;

    public AmazonTaskFetcher(String accessKey, String secretKey, String associateTag) {
        initRequestHelper(accessKey, secretKey, associateTag);
        this.associateTag = associateTag;
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

        run(task, consumer);

        this.connectTime += Instant.now().toEpochMilli() - start.toEpochMilli();
    }

    private void run(AmazonTask task, Consumer<AmazonTask> consumer) {
        totalTaskCount++;
        String requestUrl = getRequestUrl(task);

        while (task.getErrorCount() <= task.getMaxRetryCount()) {
            try {
                LOGGER.debug("[Amazon更新器][正在更新Amazon数据][ASIN={}][RETRY={}]",
                        task.getAsin(), task.getErrorCount());
                totalConnectCount++;
                Response response = Jsoup.connect(requestUrl)
                        .ignoreHttpErrors(true)
                        .ignoreContentType(true)
                        .execute();
                if (response.statusCode() == 200) {
                    byte[] bytes = response.bodyAsBytes();
                    task.setDocument(dbf.newDocumentBuilder().parse(new ByteArrayInputStream(bytes)));
                    task.setDone(true);
                    task.setFinishTime(LocalDateTime.now().withNano(0));
                    break;
                }

                task.setErrorCount(task.getErrorCount() + 1);
                task.setErrorMessage(String.format("%d: %s", response.statusCode(), response.statusMessage()));
                if (response.statusCode() == 503) {
                    er503ConnectCount++;
                    threadSleep(1000);
                } else if (response.statusCode() == 400) {
                    er400ConnectCount++;
                    LOGGER.warn("[Amazon更新器][更新Amazon数据错误][ASIN={}][RETRY={}][AMAZON 400 ERROR]",
                            task.getAsin(), task.getErrorCount());
                } else {
                    errorConnectCount++;
                    LOGGER.warn("[Amazon更新器][更新Amazon数据错误][ASIN={}][RETRY={}][AMAZON {} ERROR]",
                            task.getAsin(), task.getErrorCount(), response.statusCode());
                }
            } catch (Exception e) {
                task.setErrorCount(task.getErrorCount() + 1);
                task.setErrorMessage(String.format("%s: %s", e.getClass().getName(), e.getMessage()));
                System.out.println(task.getErrorMessage());
                errorConnectCount++;

                if (!(e instanceof SocketTimeoutException)) {
                    LOGGER.warn("[Amazon更新器][更新Amazon数据错误][ASIN={}][RETRY={}][ERROR={}]",
                            task.getAsin(), task.getErrorCount(), e.getClass());
                    LOGGER.debug("[Amazon更新器][更新Amazon数据错误]", e);
                }
            }
        }
        if (!task.isDone()) {
            errorTaskCount++;
            LOGGER.debug("[Amazon更新器][更新Amazon数据失败][ASIN={}][RETRY={}]",
                    task.getAsin(), task.getErrorCount());
        } else {
            LOGGER.debug("[Amazon更新器][成功更新Amazon数据][ASIN={}][RETRY={}]",
                    task.getAsin(), task.getErrorCount());
        }
        consumer.accept(task);
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

    public String getAssociateTag() {
        return associateTag;
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

    public long getEr400ConnectCount() {
        return er400ConnectCount;
    }

    public long computeCostPerTask() {
        if (totalTaskCount == 0) {
            return 0;
        }
        return connectTime / totalTaskCount;
    }
}
