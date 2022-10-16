package com.mingzuozhibi.modules.admin;

import com.mingzuozhibi.commons.base.BaseController;
import com.mingzuozhibi.commons.base.BaseKeys.Name;
import com.mingzuozhibi.commons.logger.LoggerBind;
import com.mingzuozhibi.commons.utils.EnvLoader;
import com.mingzuozhibi.modules.disc.DiscRepository;
import com.mingzuozhibi.modules.record.RecordCompute;
import com.mingzuozhibi.modules.vultr.VultrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import static com.mingzuozhibi.commons.base.BaseController.DEFAULT_TYPE;
import static com.mingzuozhibi.commons.utils.MyTimeUtils.fmtDate;
import static com.mingzuozhibi.commons.utils.ThreadUtils.runWithDaemon;

@LoggerBind(Name.SERVER_CORE)
@Transactional
@RestController
@RequestMapping(produces = DEFAULT_TYPE)
public class AdminController extends BaseController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private VultrService vultrService;

    @Autowired
    private RecordCompute recordCompute;

    @Autowired
    private DiscRepository discRepository;

    @Scheduled(cron = "0 0 * * * ?")
    @GetMapping("/admin/startAutoTask")
    public void startAutoTask() {
        runWithDaemon(bind, "每小时自动任务", () -> {
            adminService.deleteExpiredRemembers();
            adminService.moveExpiredHourRecords();
            adminService.recordRankAndComputePt();
            adminService.cleanupModulesMessages();
        });
    }

    @Scheduled(cron = "0 2 0/6 * * ?")
    @GetMapping("/admin/createServer")
    public void createServer() {
        if (EnvLoader.isDevMode()) {
            bind.info("In development mode, stop createServer()");
            return;
        }
        runWithDaemon(bind, "创建抓取服务器", () -> {
            vultrService.setRetry(1);
            vultrService.createServer();
        });
    }

    @GetMapping("/admin/reComputeDate/{date}")
    public void reComputeDate(@PathVariable String date) {
        recordCompute.computeDate(LocalDate.parse(date, fmtDate));
    }

    @GetMapping("/admin/reComputeDisc/{id}")
    public void reComputeDisc(@PathVariable Long id) {
        discRepository.findById(id).ifPresent(disc -> {
            recordCompute.computeDisc(disc);
        });
    }

}
