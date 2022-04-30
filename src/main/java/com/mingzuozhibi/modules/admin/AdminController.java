package com.mingzuozhibi.modules.admin;

import com.mingzuozhibi.commons.base.BaseController;
import com.mingzuozhibi.commons.mylog.JmsEnums.Name;
import com.mingzuozhibi.commons.mylog.JmsLogger;
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
        jmsSender.send(NEED_UPDATE_ASINS, gson.toJson(asins));
        JmsLogger bind = jmsSender.bind(Name.SERVER_DISC);
        bind.debug("JMS -> %s size=%d", NEED_UPDATE_ASINS, asins.size());
    }

    @Transactional
    @Scheduled(cron = "0 0 * * * ?")
    @GetMapping(value = "/admin/runAutomaticTasks", produces = MEDIA_TYPE)
    public void runAutomaticTasks() {
        JmsLogger bind = jmsSender.bind(Name.SERVER_CORE);
        runWithDaemon("自动任务", bind, () -> {
            adminService.deleteExpiredRemembers();
            adminService.moveExpiredHourRecords();
            adminService.recordRankAndComputePt();
        });
    }

}
