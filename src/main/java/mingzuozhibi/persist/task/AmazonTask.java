package mingzuozhibi.persist.task;

import javax.persistence.Column;
import java.time.LocalDateTime;
import java.util.Objects;

public class AmazonTask {

    private String asin;
    private String body;
    private boolean isDone;
    private int errorCount;
    private int maxRetryCount;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime finishTime;

    public AmazonTask() {
    }

    public AmazonTask(String asin, int maxRetryCount) {
        this.asin = asin;
        this.maxRetryCount = maxRetryCount;
        this.createTime = LocalDateTime.now().withNano(0);
    }

    @Column(length = 10, unique = true, nullable = false)
    public String getAsin() {
        return asin;
    }

    public void setAsin(String asin) {
        this.asin = asin;
    }

    @Column(length = 1000)
    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Column(nullable = false)
    public boolean isDone() {
        return isDone;
    }

    public void setDone(boolean done) {
        isDone = done;
    }

    @Column(nullable = false)
    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    @Column(nullable = false)
    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    @Column(length = 1000)
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Column(nullable = false)
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Column
    public LocalDateTime getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(LocalDateTime finishTime) {
        this.finishTime = finishTime;
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
