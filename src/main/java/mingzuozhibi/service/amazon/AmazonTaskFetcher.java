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
    private String associateTag;

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
                totalConnectCount++;
                task.setDocument(getDocument(requestUrl));
                task.setDone(true);
                task.setFinishTime(LocalDateTime.now().withNano(0));
                consumer.accept(task);
                return;
            } catch (IOException e) {
                if (e.getMessage().startsWith("Server returned HTTP response code: 503")) {
                    er503ConnectCount++;
                    threadSleep(1000);
                    continue;
                }
                if (e.getMessage().startsWith("Server returned HTTP response code: 400")) {
                    er400ConnectCount++;
                    task.setErrorCount(task.getErrorCount() + 1);
                    task.setErrorMessage(e.getMessage());
                } else {
                    errorConnectCount++;
                    task.setErrorCount(task.getErrorCount() + 1);
                    task.setErrorMessage(e.getMessage());
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

    public long getErdocConnectCount() {
        return erdocConnectCount;
    }

    public long computeCostPerTask() {
        if (totalTaskCount == 0) {
            return 0;
        }
        return connectTime / totalTaskCount;
    }
}
