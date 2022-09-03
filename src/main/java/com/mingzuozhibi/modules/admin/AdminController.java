package com.mingzuozhibi.modules.admin;

import com.mingzuozhibi.commons.base.BaseKeys.Name;
import com.mingzuozhibi.commons.logger.LoggerBind;
import com.mingzuozhibi.commons.base.BaseController;
import com.mingzuozhibi.modules.disc.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.mingzuozhibi.commons.utils.ThreadUtils.runWithDaemon;

@RestController
@LoggerBind(Name.SERVER_CORE)
public class AdminController extends BaseController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private VultrService vultrService;

    @Scheduled(cron = "0 0 * * * ?")
    @GetMapping(value = "/admin/runAutomaticTasks", produces = MEDIA_TYPE)
    public void runAutomaticTasks() {
        runWithDaemon(bind, "每小时自动任务", () -> {
            bind.info("每小时自动任务：开始");

            adminService.deleteExpiredRemembers();
            adminService.moveExpiredHourRecords();
            adminService.recordRankAndComputePt();
            adminService.cleanupModulesMessages();

            bind.info("每小时自动任务：完成");
        });
    }

    @Scheduled(cron = "0 2 0/4 * * ?")
    @GetMapping(value = "/admin/runAutomaticTasks2", produces = MEDIA_TYPE)
    public void runAutomaticTasks2() {
        runWithDaemon(bind, "创建抓取服务器", () -> {
            bind.info("创建抓取服务器：开始");
            vultrService.createServer();
            bind.info("创建抓取服务器：完成");
        });
    }

}
