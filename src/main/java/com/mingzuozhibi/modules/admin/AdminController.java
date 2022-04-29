package com.mingzuozhibi.modules.admin;

import com.mingzuozhibi.commons.base.BaseController;
import com.mingzuozhibi.commons.mylog.JmsEnums.Name;
import com.mingzuozhibi.modules.disc.GroupService;
import com.mingzuozhibi.utils.ThreadUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
public class AdminController extends BaseController {

    @Autowired
    private GroupService groupService;

    @Autowired
    private AdminService adminService;

    @Transactional
    @Scheduled(cron = "0 59 * * * ?")
    @GetMapping(value = "/admin/sendNeedUpdateAsins", produces = MEDIA_TYPE)
    public void sendNeedUpdateAsins() {
        Set<String> asins = groupService.findNeedUpdateAsinsSorted();
        jmsSender.send("need.update.asins", gson.toJson(asins));
        jmsSender.bind(Name.SERVER_CORE)
            .info("JMS -> need.update.asins: size=%d", asins.size());
    }

    @Transactional
    @Scheduled(cron = "0 0 * * * ?")
    @GetMapping(value = "/admin/runAutomaticTasks", produces = MEDIA_TYPE)
    public void runAutomaticTasks() {
        jmsSender.bind(Name.SERVER_CORE)
            .info("运行每小时自动任务");
        ThreadUtils.startThread(() -> {
            adminService.deleteExpiredRemembers();
            adminService.moveExpiredHourRecords();
            adminService.recordRankAndComputePt();
        });
    }

}
