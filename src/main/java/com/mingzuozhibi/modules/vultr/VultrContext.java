package com.mingzuozhibi.modules.vultr;

import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.base.BaseKeys.Name;
import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.commons.logger.LoggerBind;
import com.mingzuozhibi.modules.core.VarableService;
import com.mingzuozhibi.modules.vultr.VultrLoader.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static com.mingzuozhibi.commons.utils.FormatUtils.fmtDateTime2;
import static com.mingzuozhibi.support.FileIoUtils.writeLine;

@Slf4j
@Component
@LoggerBind(Name.SERVER_CORE)
public class VultrContext extends BaseSupport {

    private static final List<Region> REGIONS = VultrLoader.loadRegions();

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

    public void setRegionIndex(int regionIndex) {
        this.regionIndex = regionIndex % REGIONS.size();
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

    @PostConstruct
    public void init() {
        varableService.findIntegerByKey(REGION_INDEX)
            .ifPresent(this::setRegionIndex);
        varableService.findIntegerByKey(TASK_COUNT)
            .ifPresent(this::setTaskCount);
        varableService.findIntegerByKey(DONE_COUNT)
            .ifPresent(this::setDoneCount);
        log.info("Vultr Instance Region = %s".formatted(formatRegion()));
    }

    public String useCode() {
        String region = REGIONS.get(regionIndex).getCode();
        setRegionIndex(regionIndex + 1);
        return region;
    }

    public void printStatus(JsonObject instance) {
        var mainIp = instance.get("main_ip").getAsString();
        var region = instance.get("region").getAsString();
        var skipCount = taskCount - doneCount;
        var status = "mainIp = %s, region = %s, task = %d, done = %d, skip = %d".formatted(
            mainIp, formatRegion(region), taskCount, doneCount, skipCount
        );

        if (skipCount > 25 && taskCount > 100) {
            log.warn("Instance Status: %s".formatted(status));
            bind.warning("抓取状态异常：%s".formatted(status));
        } else {
            log.info("Instance Status: %s".formatted(status));
        }

        var datetime = LocalDateTime.now().format(fmtDateTime2);
        writeLine("var/instance.log", "[%s] %s".formatted(datetime, status));
    }

    public String formatRegion() {
        return formatRegion(regionIndex, REGIONS.get(regionIndex));
    }

    private String formatRegion(String code) {
        for (int index = 0; index < REGIONS.size(); index++) {
            if (Objects.equals(REGIONS.get(index).getCode(), code)) {
                return formatRegion(index, REGIONS.get(index));
            }
        }
        return "Unknown";
    }

    private String formatRegion(int index, Region region) {
        return "%d (%s/%s)".formatted(index, region.getCity(), region.getArea());
    }

}
