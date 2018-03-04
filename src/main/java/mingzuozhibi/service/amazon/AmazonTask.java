package mingzuozhibi.service.amazon;

import org.w3c.dom.Document;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Consumer;

public class AmazonTask {

    private String asin;
    private String param;
    private boolean isDone;
    private Document document;

    private int errorCount;
    private int maxRetryCount;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime finishTime;
    private Consumer<AmazonTask> consumer;

    public AmazonTask() {
    }

    public AmazonTask(String asin, String param, Consumer<AmazonTask> consumer, int maxRetryCount) {
        this.asin = asin;
        this.param = param;
        this.consumer = consumer;
        this.maxRetryCount = maxRetryCount;
        this.createTime = LocalDateTime.now().withNano(0);
    }

    public String getAsin() {
        return asin;
    }

    public void setAsin(String asin) {
        this.asin = asin;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }

    public boolean isDone() {
        return isDone;
    }

    public void setDone(boolean done) {
        isDone = done;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(LocalDateTime finishTime) {
        this.finishTime = finishTime;
    }

    public Consumer<AmazonTask> getConsumer() {
        return consumer;
    }

    public void setConsumer(Consumer<AmazonTask> consumer) {
        this.consumer = consumer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AmazonTask that = (AmazonTask) o;
        return Objects.equals(asin, that.asin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(asin);
    }

}
