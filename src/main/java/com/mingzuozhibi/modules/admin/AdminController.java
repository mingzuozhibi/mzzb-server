package com.mingzuozhibi.modules.admin;

import com.mingzuozhibi.commons.base.BaseController;
import com.mingzuozhibi.commons.mylog.JmsBind;
import com.mingzuozhibi.commons.mylog.JmsEnums.Name;
import com.mingzuozhibi.modules.disc.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

import static com.mingzuozhibi.commons.mylog.JmsEnums.NEED_UPDATE_ASINS;
import static com.mingzuozhibi.commons.utils.ThreadUtils.runWithDaemon;

@RestController
@JmsBind(Name.SERVER_CORE)
public class AdminController extends BaseController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private GroupService groupService;

    @Scheduled(cron = "0 0 * * * ?")
    @GetMapping(value = "/admin/runAutomaticTasks", produces = MEDIA_TYPE)
    public void runAutomaticTasks() {
        runWithDaemon(bind, "每1小时自动任务", () -> {
            bind.info("每1小时自动任务：开始");
            adminService.moveExpiredHourRecords();
            adminService.recordRankAndComputePt();
            bind.info("每1小时自动任务：完成");
        });
    }

    @Scheduled(cron = "0 8 1/4 * * ?")
    @GetMapping(value = "/admin/runAutomaticTasks2", produces = MEDIA_TYPE)
    public void runAutomaticTasks2() {
        runWithDaemon(bind, "每4小时自动任务", () -> {
            bind.info("每4小时自动任务：开始");
            adminService.deleteExpiredRemembers();
            adminService.cleanupModulesMessages();
            bind.info("每4小时自动任务：完成");
        });
    }

    @Transactional
    @Scheduled(cron = "0 10 * * * ?")
    @GetMapping(value = "/admin/sendNeedUpdateAsins", produces = MEDIA_TYPE)
    public void sendNeedUpdateAsins() {
        Set<String> asins = groupService.findNeedUpdateAsinsSorted();
        jmsSender.send(NEED_UPDATE_ASINS, gson.toJson(asins));
        bind.debug("JMS -> %s size=%d", NEED_UPDATE_ASINS, asins.size());
    }

}
