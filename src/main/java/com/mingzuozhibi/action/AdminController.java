package com.mingzuozhibi.action;

import com.mingzuozhibi.commons.base.BaseController;
import com.mingzuozhibi.commons.mylog.JmsService;
import com.mingzuozhibi.modules.disc.Disc;
import com.mingzuozhibi.utils.ReCompute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import static com.mingzuozhibi.commons.utils.FormatUtils.DATE_FORMATTER;

@RestController
public class AdminController extends BaseController {

    @Autowired
    private ReCompute reCompute;

    @Autowired
    private JmsService jmsService;

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/api/admin/reCompute/date/{date}", produces = MEDIA_TYPE)
    public String reCompute(@PathVariable String date) {
        return localCompute(date);
    }

    @Transactional
    @GetMapping(value = "/admin/reCompute/date/{date}", produces = MEDIA_TYPE)
    public String localCompute(@PathVariable String date) {
        try {
            LocalDate localDate = LocalDate.parse(date, DATE_FORMATTER);
            reCompute.reComputeDateRecords(localDate);
            return objectResult("done");
        } catch (RuntimeException e) {
            return errorMessage(e.getMessage());
        }
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/api/admin/reCompute/disc/{id}", produces = MEDIA_TYPE)
    public String reCompute(@PathVariable Long id) {
        return localCompute(id);
    }

    @Transactional
    @GetMapping(value = "/admin/reCompute/disc/{id}", produces = MEDIA_TYPE)
    public String localCompute(@PathVariable Long id) {
        try {
            Disc disc = dao.get(Disc.class, id);
            reCompute.reComputeDateRecords(disc);
            return objectResult("done");
        } catch (RuntimeException e) {
            return errorMessage(e.getMessage());
        }
    }

}
