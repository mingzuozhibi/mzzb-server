package mingzuozhibi.action;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.utils.JmsHelper;
import mingzuozhibi.utils.ReCompute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
public class AdminController extends BaseController {

    @Autowired
    private ReCompute reCompute;

    @Autowired
    private JmsHelper jmsHelper;

    @Transactional
    @PreAuthorize("hasRole('Root_Admin')")
    @PostMapping(value = "/api/admin/reCompute/date/{date}", produces = MEDIA_TYPE)
    public String reCompute(@PathVariable String date) {
        return localCompute(date);
    }

    @Transactional
    @GetMapping(value = "/admin/reCompute/date/{date}", produces = MEDIA_TYPE)
    public String localCompute(@PathVariable String date) {
        try {
            LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            reCompute.reComputeDateRecords(localDate);
            return objectResult("done");
        } catch (RuntimeException e) {
            return errorMessage(e.getMessage());
        }
    }

    @Transactional
    @PreAuthorize("hasRole('Root_Admin')")
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

    @Transactional
    @GetMapping(value = "/admin/reSendDiscTrack", produces = MEDIA_TYPE)
    public String reSendDiscTrack() {
        try {
            dao.jdbc(connection -> {
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery("select asin, title from disc");
                while (rs.next()) {
                    jmsHelper.sendDiscTrack(rs.getString("asin"), rs.getString("title"));
                }
                return null;
            });
            return objectResult("done");
        } catch (RuntimeException e) {
            return errorMessage(e.getMessage());
        }
    }

}
