package mingzuozhibi.action;

import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.service.AmazonDiscSpider;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static mingzuozhibi.utils.DiscUtils.needUpdateAsins;

@RestController
public class AdminController extends BaseController {

    @Deprecated
    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @GetMapping(value = "/api/discs/activeCount")
    public String deprecatedFetchCount() {
        return fetchCount();
    }

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

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Autowired
    private AmazonDiscSpider amazonDiscSpider;

    @Deprecated
    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @GetMapping(value = "/api/discs/search/{asin}", produces = MEDIA_TYPE)
    public String searchDiscDeprecated(@PathVariable String asin) {
        return searchDisc(asin);
    }

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
        JSONObject result = amazonDiscSpider.searchDisc(asin);
        if (!result.getBoolean("success")) {
            return result.toString();
        }
        Disc disc = createDisc(asin, result.getJSONObject("data"));
        dao.save(disc);

        JSONObject data = disc.toJSON();
        if (LOGGER.isInfoEnabled()) {
            infoRequest("[申请查询碟片][成功从日亚查询][ASIN={}][JSON={}]", asin, data);
        }
        return objectResult(data);
    }

    private Disc createDisc(String asin, JSONObject discJson) {
        return new Disc(
                asin,
                discJson.getString("title"),
                Disc.DiscType.valueOf(discJson.getString("type")),
                Disc.UpdateType.Both,
                false,
                LocalDate.parse(discJson.getString("date"), formatter)
        );
    }

}
