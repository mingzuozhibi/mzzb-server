package com.mingzuozhibi.modules.admin;

import com.mingzuozhibi.commons.base.BaseController;
import com.mingzuozhibi.commons.base.BaseKeys.Name;
import com.mingzuozhibi.commons.logger.LoggerBind;
import com.mingzuozhibi.modules.vultr.VultrService;
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
    private VultrService vultrService;

    @Scheduled(cron = "0 0 * * * ?")
    @GetMapping(value = "/admin/startAutoTask", produces = MEDIA_TYPE)
    public void startAutoTask() {
        runWithDaemon(bind, "每小时自动任务", () -> {
            adminService.deleteExpiredRemembers();
            adminService.moveExpiredHourRecords();
            adminService.recordRankAndComputePt();
            adminService.cleanupModulesMessages();
        });
    }

    @Scheduled(cron = "0 2 0/4 * * ?")
    @GetMapping(value = "/admin/createServer", produces = MEDIA_TYPE)
    public void createServer() {
        runWithDaemon(bind, "创建抓取服务器", () -> {
            vultrService.createServer();
        });
    }

}
