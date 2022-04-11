package com.mingzuozhibi.modules.admin;

import com.mingzuozhibi.commons.base.BaseController;
import com.mingzuozhibi.commons.mylog.JmsService;
import com.mingzuozhibi.modules.disc.Disc;
import com.mingzuozhibi.modules.disc.DiscRepository;
import com.mingzuozhibi.modules.group.DiscGroupService;
import com.mingzuozhibi.modules.record.RecordCompute;
import com.mingzuozhibi.utils.ThreadUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static com.mingzuozhibi.utils.ChecksUtils.paramNotExists;
import static com.mingzuozhibi.utils.FormatUtils.DATE_FORMATTER;
import static com.mingzuozhibi.utils.ModifyUtils.logUpdate;

@RestController
public class AdminController extends BaseController {

    @Autowired
    private JmsService jmsService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AdminService adminService;

    @Autowired
    private RecordCompute recordCompute;

    @Autowired
    private DiscRepository discRepository;

    @Autowired
    private DiscGroupService discGroupService;

    @Transactional
    @Scheduled(cron = "0 59 * * * ?")
    @GetMapping(value = "/admin/sendNeedUpdateAsins", produces = MEDIA_TYPE)
    public void sendNeedUpdateAsins() {
        Set<String> asins = discGroupService.findNeedUpdateAsins();
        jmsService.convertAndSend("need.update.asins", gson.toJson(asins));
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

    @Transactional
    @GetMapping(value = "/admin/reComputeDate/{date}", produces = MEDIA_TYPE)
    public void reComputeDate(@PathVariable String date) {
        recordCompute.computeDate(LocalDate.parse(date, DATE_FORMATTER));
    }

    @Transactional
    @GetMapping(value = "/admin/reComputeDisc/{id}", produces = MEDIA_TYPE)
    public void reComputeDisc(@PathVariable Long id) {
        discRepository.findById(id).ifPresent(disc -> {
            recordCompute.computeDisc(disc);
        });
    }

    @Transactional
    @PostMapping(value = "/api/admin/reComputeDisc2/{id}", produces = MEDIA_TYPE)
    public String reComputeDisc2(@PathVariable Long id) {
        Optional<Disc> byId = discRepository.findById(id);
        if (!byId.isPresent()) {
            return paramNotExists("碟片ID");
        }
        Disc disc = byId.get();
        Integer pt1 = disc.getTotalPt();
        recordCompute.computeDisc(disc);
        Integer pt2 = disc.getTotalPt();
        jmsMessage.notify(logUpdate("碟片PT", pt1, pt2));
        return dataResult("compute: " + pt1 + "->" + pt2);
    }

}
