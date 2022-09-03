package com.mingzuozhibi.modules.admin;

import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.base.BaseKeys.Name;
import com.mingzuozhibi.commons.logger.LoggerBind;
import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.modules.core.VarableService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;

import static com.mingzuozhibi.commons.utils.FormatUtils.fmtDateTime2;
import static com.mingzuozhibi.support.FileIoUtils.writeLine;

@Slf4j
@Component
@LoggerBind(Name.SERVER_CORE)
public class VultrContext extends BaseSupport {

    private static final String[] REGIONS =
        {
            "atl", "dfw", "ewr", "lax", "mia", "ord", "sea", "sjc", "mex", "yto",
            "ams", "cdg", "fra", "lhr", "sto", "waw", "mel", "syd", "sgp", "nrt"
        };

    private static final String REGION_INDEX = "VultrService.regionIndex";
    private static final String TASK_COUNT = "VultrService.taskCount";
    private static final String DONE_COUNT = "VultrService.doneCount";
    private static final String STARTTED = "VultrService.startted";
    private static final String TIMEOUT = "VultrService.timeout";

    @Autowired
    private VarableService varableService;

    @Getter
    private int regionIndex;

    @Getter
    private int taskCount;

    @Getter
    private int doneCount;

    @Getter
    private boolean startted;

    @Getter
    private Instant timeout;

    public void setRegionIndex(int regionIndex) {
        this.regionIndex = regionIndex % REGIONS.length;
        varableService.saveOrUpdate(REGION_INDEX, "%d".formatted(this.regionIndex));
    }

    public void setTaskCount(int taskCount) {
        this.taskCount = taskCount;
        varableService.saveOrUpdate(TASK_COUNT, "%d".formatted(this.taskCount));
    }

    public void setDoneCount(int doneCount) {
        this.doneCount = doneCount;
        varableService.saveOrUpdate(DONE_COUNT, "%d".formatted(this.doneCount));
    }

    public void setStartted(boolean startted) {
        this.startted = startted;
        varableService.saveOrUpdate(STARTTED, "%b".formatted(this.startted));
    }

    public void setTimeout(Instant timeout) {
        this.timeout = timeout;
        varableService.saveOrUpdate(TIMEOUT, "%d".formatted(this.timeout.toEpochMilli()));
    }

    public void init() {
        varableService.findIntegerByKey(REGION_INDEX)
            .ifPresent(this::setRegionIndex);
        varableService.findIntegerByKey(TASK_COUNT)
            .ifPresent(this::setTaskCount);
        varableService.findIntegerByKey(DONE_COUNT)
            .ifPresent(this::setDoneCount);
        varableService.findByKey(STARTTED)
            .map(Boolean::valueOf)
            .ifPresent(this::setStartted);
        varableService.findByKey(TIMEOUT)
            .map(Long::valueOf)
            .map(Instant::ofEpochMilli)
            .ifPresent(this::setTimeout);
    }

    public String formatRegion() {
        return "%d (%s)".formatted(regionIndex, REGIONS[regionIndex]);
    }

    public String nextRegion() {
        String region = REGIONS[regionIndex];
        setRegionIndex(regionIndex + 1);
        return region;
    }

    public void printStatus(JsonObject instance) {
        var mainIp = instance.get("main_ip").getAsString();
        var region = instance.get("region").getAsString();
        var nextCount = taskCount - doneCount;
        var index = ArrayUtils.indexOf(REGIONS, region);
        var status = "mainIp = %s, region = %d(%s), task = %d, done = %d, next = %d".formatted(
            mainIp, index, region, taskCount, doneCount, nextCount
        );

        if (nextCount > 25 && taskCount > 100) {
            log.warn("Instance Status: %s".formatted(status));
            bind.warning("抓取状态异常：%s".formatted(status));
        } else {
            log.info("Instance Status: %s".formatted(status));
        }

        var datetime = LocalDateTime.now().format(fmtDateTime2);
        writeLine("var/instance.log", "[%s] %s".formatted(datetime, status));
    }

}
