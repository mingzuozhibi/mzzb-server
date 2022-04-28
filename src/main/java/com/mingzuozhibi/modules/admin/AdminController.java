package com.mingzuozhibi.modules.admin;

import com.mingzuozhibi.commons.base.BaseController;
import com.mingzuozhibi.commons.mylog.JmsService;
import com.mingzuozhibi.modules.disc.Disc;
import com.mingzuozhibi.modules.disc.DiscRepository;
import com.mingzuozhibi.modules.group.DiscGroupService;
import com.mingzuozhibi.modules.record.RecordCompute;
import com.mingzuozhibi.utils.ThreadUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static com.mingzuozhibi.commons.gson.GsonFactory.GSON;
import static com.mingzuozhibi.commons.utils.FormatUtils.fmtDate;
import static com.mingzuozhibi.utils.ChecksUtils.paramNotExists;
import static com.mingzuozhibi.utils.ModifyUtils.logUpdate;

@RestController
public class AdminController extends BaseController {

    @Autowired
    private JmsService jmsService;

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
        Set<String> asins = discGroupService.findNeedUpdateAsinsSorted();
        jmsService.convertAndSend("need.update.asins", GSON.toJson(asins));
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
    @GetMapping(value = "/admin/reComputeDate/{date}", produces = MEDIA_TYPE)
    public void reComputeDate(@PathVariable String date) {
        recordCompute.computeDate(LocalDate.parse(date, fmtDate));
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
        jmsMessage.notify(logUpdate("碟片PT", pt1, pt2, disc.getLogName()));
        return dataResult("compute: " + pt1 + "->" + pt2);
    }

}
