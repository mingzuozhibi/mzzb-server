package com.mingzuozhibi.modules.vultr;

import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.base.BaseKeys.Name;
import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.commons.logger.LoggerBind;
import com.mingzuozhibi.modules.core.VarBean;
import com.mingzuozhibi.modules.core.VarableService;
import com.mingzuozhibi.modules.vultr.sdk.MetaLoader;
import com.mingzuozhibi.modules.vultr.sdk.MetaLoader.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static com.mingzuozhibi.commons.utils.MyTimeUtils.*;
import static com.mingzuozhibi.support.FileIoUtils.writeLine;

@Slf4j
@Component
@LoggerBind(Name.SERVER_CORE)
public class VultrContext extends BaseSupport {

    private static final List<Region> REGIONS = MetaLoader.loadRegions();
    private static final String KEY_REGION_IDX = "VultrService.regionIndex";
    private static final String KEY_TASK_COUNT = "VultrService.taskCount";
    private static final String KEY_DONE_COUNT = "VultrService.doneCount";
    private static final String KEY_STARTTED = "VultrService.startted";
    private static final String KEY_TIMEOUT = "VultrService.timeout";
    private static final String KEY_RETRY = "VultrService.retry";
    private static final String KEY_DISABLE = "VultrService.disable";

    @Autowired
    private VarableService varableService;

    @Getter
    private VarBean<Integer> regionIdx;
    @Getter
    private VarBean<Integer> taskCount;
    @Getter
    private VarBean<Integer> doneCount;
    @Getter
    private VarBean<Boolean> startted;
    @Getter
    private VarBean<Instant> timeout;
    @Getter
    private VarBean<Integer> retry;
    @Getter
    private VarBean<Boolean> disable;

    public void init() {
        regionIdx = varableService.createInteger(KEY_REGION_IDX, 0);
        taskCount = varableService.createInteger(KEY_TASK_COUNT, 0);
        doneCount = varableService.createInteger(KEY_DONE_COUNT, 0);
        startted = varableService.createBoolean(KEY_STARTTED, true);
        timeout = varableService.create(KEY_TIMEOUT, Instant.now(),
            instant -> String.valueOf(instant.toEpochMilli()),
            string -> Instant.ofEpochMilli(Long.parseLong(string)));
        retry = varableService.createInteger(KEY_RETRY, 0);
        disable = varableService.createBoolean(KEY_DISABLE, false);

        log.info("Vultr Instance Region = %s".formatted(formatRegion()));
        log.info("Vultr Instance Startted = %b".formatted(startted.getValue()));
        if (!startted.getValue()) {
            var timeout = ofInstant(this.timeout.getValue());
            log.info("Vultr Instance Timeout = %s".formatted(fmtDateTime.format(timeout)));
            log.info("Vultr Instance Retry = %d".formatted(retry.getValue()));
        }
        log.info("Vultr Instance Disable = %b".formatted(disable.getValue()));
    }

    public String nextCode() {
        int regionIdx = this.regionIdx.getValue();
        var nextIndex = (regionIdx + 1) % REGIONS.size();
        this.regionIdx.setValue(nextIndex);
        return REGIONS.get(regionIdx).getCode();
    }

    public void printStatus(JsonObject instance) {
        var taskCount = this.taskCount.getValue();
        var doneCount = this.doneCount.getValue();

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

        var datetime = LocalDateTime.now().format(fmtDateTime);
        writeLine("var/instance.log", "[%s] %s".formatted(datetime, status));
    }

    public String formatRegion() {
        var regionIdx = this.regionIdx.getValue();
        return formatRegion(regionIdx, REGIONS.get(regionIdx));
    }

    private String formatRegion(String code) {
        for (var index = 0; index < REGIONS.size(); index++) {
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
