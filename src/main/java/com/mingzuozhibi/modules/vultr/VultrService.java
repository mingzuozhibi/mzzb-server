package com.mingzuozhibi.modules.vultr;

import com.mingzuozhibi.commons.base.BaseController;
import com.mingzuozhibi.commons.logger.LoggerBind;
import com.mingzuozhibi.commons.utils.*;
import com.mingzuozhibi.modules.disc.DiscRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.mingzuozhibi.commons.base.BaseKeys.*;
import static com.mingzuozhibi.commons.utils.MyTimeUtils.fmtDateTime;
import static com.mingzuozhibi.commons.utils.ThreadUtils.runWithDaemon;

@Slf4j
@Service
@LoggerBind(Name.SERVER_CORE)
public class VultrService extends BaseController {

    @Autowired
    private VultrApi vultrApi;

    @Autowired
    private VultrContext vultrContext;

    @Autowired
    private DiscRepository discRepository;

    @PostConstruct
    public void init() {
        vultrApi.init();
        vultrContext.init();
        initCheck();
    }

    private void initCheck() {
        if (!vultrContext.getStartted().getValue()) {
            if (EnvLoader.isDevMode()) {
                log.info("In development mode, stop checkServer()");
            } else {
                checkServer(vultrContext.getTimeout().getValue());
            }
        }
    }

    private void checkServer(Instant timeout) {
        log.info("Vultr Instance Timeout = %s".formatted(
            MyTimeUtils.ofInstant(timeout).format(fmtDateTime)
        ));
        runWithDaemon(bind, "检查服务器超时", () -> {
            while (true) {
                var millis = timeout.toEpochMilli() - Instant.now().toEpochMilli();
                if (millis <= 0) {
                    if (vultrContext.getStartted().getValue()) {
                        bind.success("服务器正常运行中");
                    } else {
                        bind.warning("服务器似乎已超时，重新开始任务");
                        tryRedoTask();
                    }
                    break;
                }
                ThreadUtils.sleepMillis(millis);
            }
        });
    }

    public void createServer() {
        if (vultrContext.getDisable().getValue()) {
            bind.info("系统已设定为禁止更新");
            return;
        }
        var tasks = discRepository.findNeedUpdate().stream()
            .map(disc -> new TaskOfContent(disc.getAsin(), disc.getThisRank()))
            .collect(Collectors.toList());

        if (createInstance()) {
            vultrContext.getTaskCount().setValue(tasks.size());
            vultrContext.getDoneCount().setValue(0);
            vultrContext.getStartted().setValue(false);
            vultrContext.getTimeout().setValue(Instant.now().plus(15, ChronoUnit.MINUTES));
            checkServer(vultrContext.getTimeout().getValue());
            amqpSender.send(FETCH_TASK_START, gson.toJson(tasks));
            bind.info("JMS -> %s size=%d".formatted(FETCH_TASK_START, tasks.size()));
        }
    }

    public void finishServer(int doneCount) {
        vultrContext.getDoneCount().setValue(doneCount);
        var taskCount = vultrContext.getTaskCount().getValue();
        var skipCount = taskCount - doneCount;
        if (skipCount <= 100) {
            bind.success("更新日亚排名成功");
            deleteInstance();
            bind.debug("Next Vultr Instance Region = %s".formatted(vultrContext.formatRegion()));
        } else {
            bind.warning("更新日亚排名失败");
            Optional.ofNullable(vultrContext.getTimeout().getValue()).ifPresent(timeout -> {
                if (Instant.now().isBefore(timeout)) {
                    setStartted(false);
                } else {
                    tryRedoTask();
                }
            });
        }
    }

    public void setStartted(boolean startted) {
        vultrContext.getStartted().setValue(startted);
    }

    public void setRetry(int retry) {
        vultrContext.getRetry().setValue(retry);
    }

    private void tryRedoTask() {
        if (deleteInstance()) {
            var retry = vultrContext.getRetry().getValue();
            if (retry > 0) {
                vultrContext.getRetry().setValue(retry - 1);
                waitForDelete();
                createServer();
            } else {
                bind.warning("重试次数已耗尽");
            }
        } else {
            bind.warning("未能重新开始任务");
        }
    }

    private void waitForDelete() {
        bind.debug("等待60秒以重新开始任务");
        ThreadUtils.sleepSeconds(60);
    }

    private boolean createInstance() {
        try {
            bind.notify("开始创建服务器");

            bind.debug("正在检查服务器");
            var instance = vultrApi.getInstance();
            if (instance.isPresent()) {
                bind.debug("检查到服务器已存在");
                if (deleteInstance()) {
                    waitForDelete();
                } else {
                    return false;
                }
            }

            bind.debug("正在获取快照ID");
            var snapshotId = vultrApi.getSnapshotId();
            if (snapshotId.isEmpty()) {
                bind.warning("未能获取快照ID");
                return false;
            }

            bind.debug("正在获取防火墙策略ID");
            var firewallId = vultrApi.getFirewallId();
            if (firewallId.isEmpty()) {
                bind.warning("未能获取防火墙策略ID");
                return false;
            }

            bind.debug("This Vultr Instance Region = %s".formatted(vultrContext.formatRegion()));
            return vultrApi.createInstance(vultrContext.nextCode(), snapshotId.get(), firewallId.get());
        } catch (Exception e) {
            bind.error("创建服务器异常：%s".formatted(e));
            return false;
        }
    }

    private boolean deleteInstance() {
        try {
            bind.notify("开始删除服务器");

            bind.debug("正在获取实例ID");
            var instance = vultrApi.getInstance();
            if (instance.isEmpty()) {
                bind.warning("未能获取实例ID");
                return false;
            }

            vultrContext.printStatus(instance.get());
            var instanceId = instance.get().get("id").getAsString();
            return vultrApi.deleteInstance(instanceId);
        } catch (Exception e) {
            bind.error("删除服务器异常：%s".formatted(e));
            return false;
        }
    }

}
