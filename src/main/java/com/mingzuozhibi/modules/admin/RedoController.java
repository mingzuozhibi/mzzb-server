package com.mingzuozhibi.modules.admin;

import com.mingzuozhibi.commons.amqp.AmqpEnums.Name;
import com.mingzuozhibi.commons.amqp.logger.LoggerBind;
import com.mingzuozhibi.commons.base.BaseController;
import com.mingzuozhibi.modules.disc.Disc;
import com.mingzuozhibi.modules.disc.DiscRepository;
import com.mingzuozhibi.modules.record.RecordCompute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;

import static com.mingzuozhibi.commons.utils.FormatUtils.fmtDate;
import static com.mingzuozhibi.support.ChecksUtils.paramNotExists;
import static com.mingzuozhibi.support.ModifyUtils.logUpdate;

@RestController
@LoggerBind(Name.SERVER_USER)
public class RedoController extends BaseController {

    @Autowired
    private RecordCompute recordCompute;

    @Autowired
    private DiscRepository discRepository;

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
        bind.notify(logUpdate("碟片PT", pt1, pt2, disc.getLogName()));
        return dataResult("compute: " + pt1 + "->" + pt2);
    }

}
