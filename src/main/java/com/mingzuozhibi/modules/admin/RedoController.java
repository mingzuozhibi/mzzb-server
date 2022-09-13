package com.mingzuozhibi.modules.admin;

import com.mingzuozhibi.commons.base.BaseController;
import com.mingzuozhibi.commons.base.BaseKeys.Name;
import com.mingzuozhibi.commons.logger.LoggerBind;
import com.mingzuozhibi.modules.disc.DiscRepository;
import com.mingzuozhibi.modules.record.RecordCompute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import static com.mingzuozhibi.commons.utils.MyTimeUtils.fmtDate;
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
    @PreAuthorize("hasRole('BASIC')")
    @PostMapping(value = "/api/admin/reComputeDisc2/{id}", produces = MEDIA_TYPE)
    public String reComputeDisc2(@PathVariable Long id) {
        var byId = discRepository.findById(id);
        if (byId.isEmpty()) {
            return paramNotExists("碟片ID");
        }
        var disc = byId.get();
        var pt1 = disc.getTotalPt();
        recordCompute.computeDisc(disc);
        var pt2 = disc.getTotalPt();
        bind.info(logUpdate("碟片PT", pt1, pt2, disc.getLogName()));
        return dataResult("compute: " + pt1 + "->" + pt2);
    }

}
