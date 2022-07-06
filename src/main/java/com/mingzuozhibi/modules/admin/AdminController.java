package com.mingzuozhibi.modules.admin;

import com.mingzuozhibi.commons.amqp.AmqpEnums.Name;
import com.mingzuozhibi.commons.amqp.logger.LoggerBind;
import com.mingzuozhibi.commons.base.BaseController;
import com.mingzuozhibi.commons.domain.Result;
import com.mingzuozhibi.modules.disc.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

import static com.mingzuozhibi.commons.amqp.AmqpEnums.NEED_UPDATE_ASINS;
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
        runWithDaemon(bind, "每1小时自动任务", () -> {
            bind.info("每1小时自动任务：开始");
            adminService.moveExpiredHourRecords();
            adminService.recordRankAndComputePt();
            bind.info("每1小时自动任务：完成");
        });
    }

    @Scheduled(cron = "0 2 0/4 * * ?")
    @GetMapping(value = "/admin/runAutomaticTasks2", produces = MEDIA_TYPE)
    public void runAutomaticTasks2() {
        runWithDaemon(bind, "每4小时自动任务", () -> {
            bind.info("每4小时自动任务：开始");

            adminService.deleteExpiredRemembers();
            adminService.cleanupModulesMessages();

            Result<String> result = vultrService.createInstance();
            if (result.isSuccess()) {
                Set<String> asins = groupService.findNeedUpdateAsinsSorted();
                amqpSender.send(NEED_UPDATE_ASINS, gson.toJson(asins));
                bind.debug("JMS -> %s size=%d", NEED_UPDATE_ASINS, asins.size());
            }

            bind.info("每4小时自动任务：完成");
        });
    }

}
