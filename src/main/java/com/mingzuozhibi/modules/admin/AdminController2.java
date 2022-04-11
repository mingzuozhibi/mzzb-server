package com.mingzuozhibi.modules.admin;

import com.mingzuozhibi.commons.base.BaseController2;
import com.mingzuozhibi.commons.mylog.JmsService;
import com.mingzuozhibi.commons.utils.ThreadUtils;
import com.mingzuozhibi.modules.group.DiscGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
public class AdminController2 extends BaseController2 {

    @Autowired
    private JmsService jmsService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AdminService adminService;

    @Autowired
    private DiscGroupService discGroupService;

    @Transactional
    @Scheduled(cron = "0 59 * * * ?")
    @GetMapping(value = "/admin/sendNeedUpdateAsins", produces = MEDIA_TYPE)
    public void sendNeedUpdateAsins() {
        Set<String> asins = discGroupService.findNeedUpdateAsins();
        jmsService.sendJson("need.update.asins", gson.toJson(asins));
        jmsMessage.notify("JMS -> need.update.asins size=" + asins.size());
    }

    @Transactional
    @Scheduled(cron = "0 0 * * * ?")
    @GetMapping(value = "/admin/runAutomaticTasks", produces = MEDIA_TYPE)
    public void runAutomaticTasks() {
        jmsMessage.notify("运行每小时自动任务");
        ThreadUtils.startThread(() -> {
            adminService.deleteExpiredRemembers();
            adminService.moveExpiredHourRecords();
            adminService.recordRankAndComputePt();
        });
    }

    @Transactional
    @GetMapping(value = "/admin/sendAllDiscTrack", produces = MEDIA_TYPE)
    public void sendAllDiscTrack() {
        jdbcTemplate.query("SELECT asin, title FROM disc", rs -> {
            while (rs.next()) {
                jmsService.sendDiscTrack(rs.getString("asin"), rs.getString("title"));
            }
        });
    }

}
