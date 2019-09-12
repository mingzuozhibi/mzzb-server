package mingzuozhibi.action;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.Disc.DiscType;
import mingzuozhibi.persist.disc.DiscShelf;
import mingzuozhibi.service.DiscInfosSpider;
import mingzuozhibi.utils.JmsHelper;
import mingzuozhibi.utils.ReCompute;
import org.json.JSONObject;
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
import java.util.Optional;

import static mingzuozhibi.utils.DiscUtils.needUpdateAsins;

@RestController
public class AdminController extends BaseController {

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @GetMapping(value = "/api/admin/fetchCount")
    public String fetchCount() {
        int fetchCount = needUpdateAsins(dao.session()).size();
        debugRequest("[fetchCount:{}]", fetchCount);
        return objectResult(fetchCount);
    }

    /*
     * begin searchDisc
     */

    @Autowired
    private JmsHelper jmsHelper;

    @Autowired
    private DiscInfosSpider discInfosSpider;

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @GetMapping(value = "/api/admin/searchDisc/{asin}", produces = MEDIA_TYPE)
    public String searchDisc(@PathVariable String asin) {
        Disc disc = dao.lookup(Disc.class, "asin", asin);
        if (disc != null) {
            if (LOGGER.isDebugEnabled()) {
                debugRequest("[申请查询碟片][碟片已存在于本地][ASIN={}]", asin);
            }
            return objectResult(disc.toJSON());
        }
        return searchDiscFromAmazon(asin);
    }

    private String searchDiscFromAmazon(@PathVariable String asin) {
        if (LOGGER.isInfoEnabled()) {
            infoRequest("[申请查询碟片][开始从日亚查询][ASIN={}]", asin);
        }
        JSONObject result = discInfosSpider.searchDisc(asin);
        if (!result.getBoolean("success")) {
            return result.toString();
        }
        Disc disc = createDisc(asin, result.getJSONObject("data"));
        updateDiscShelfFollowd(disc);

        JSONObject data = disc.toJSON();
        if (LOGGER.isInfoEnabled()) {
            infoRequest("[申请查询碟片][成功从日亚查询][ASIN={}][JSON={}]", asin, data);
        }
        return objectResult(data);
    }

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private Disc createDisc(@PathVariable String asin, JSONObject discJson) {
        Disc disc = new Disc(
                asin,
                discJson.getString("title"),
                DiscType.valueOf(discJson.getString("type")),
                createDate(discJson));
        dao.save(disc);
        return disc;
    }

    private LocalDate createDate(JSONObject discJson) {
        if (discJson.has("date")) {
            String dateString = discJson.getString("date");
            return LocalDate.parse(dateString, formatter);
        } else {
            return LocalDate.now();
        }
    }

    private void updateDiscShelfFollowd(Disc disc) {
        Optional.ofNullable(dao.lookup(DiscShelf.class, "asin", disc.getAsin())).ifPresent(discShelf -> {
            discShelf.setFollowed(true);
        });
        jmsHelper.sendDiscTrack(disc.getAsin(), disc.getTitle());
    }

    @Autowired
    private ReCompute reCompute;

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
            LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
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
