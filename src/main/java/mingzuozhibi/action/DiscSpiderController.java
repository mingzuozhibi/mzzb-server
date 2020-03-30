package mingzuozhibi.action;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import mingzuozhibi.persist.disc.Disc;
import mingzuozhibi.persist.disc.Disc.DiscType;
import mingzuozhibi.service.DiscInfo;
import mingzuozhibi.service.DiscInfosSpider;
import mingzuozhibi.utils.JmsHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static mingzuozhibi.utils.DiscUtils.needUpdateAsins;

@RestController
public class DiscSpiderController extends BaseController {

    @Autowired
    private JmsHelper jmsHelper;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private DiscInfosSpider discInfosSpider;

    private Gson gson = new Gson();

    @Scheduled(cron = "0 59 * * * ?")
    @GetMapping("/admin/sendNeedUpdateAsins")
    public void sendNeedUpdateAsins() {
        dao.execute(session -> {
            JSONArray array = new JSONArray();
            needUpdateAsins(session).forEach(array::put);
            jmsTemplate.convertAndSend("need.update.asins", array.toString());
        });
    }

    @JmsListener(destination = "prev.update.discs")
    public void discSpiderUpdate(String json) {
        Type type = new TypeToken<ArrayList<DiscInfo>>() {}.getType();
        List<DiscInfo> discInfos = gson.fromJson(json, type);
        LOGGER.info("JMS <- prev.update.discs size=" + discInfos.size());
        discInfosSpider.updateDiscInfos(discInfos);
    }

    @JmsListener(destination = "done.update.discs")
    public void discSpiderUpdate2(String json) {
        Type type = new TypeToken<ArrayList<DiscInfo>>() {}.getType();
        List<DiscInfo> discInfos = gson.fromJson(json, type);
        LOGGER.info("JMS <- done.update.discs size=" + discInfos.size());
        discInfosSpider.updateDiscInfos(discInfos);
    }

    @Transactional
    @PreAuthorize("hasRole('BASIC')")
    @GetMapping(value = "/api/admin/fetchCount")
    public String fetchCount() {
        int fetchCount = needUpdateAsins(dao.session()).size();
        debugRequest("[fetchCount:{}]", fetchCount);
        return objectResult(fetchCount);
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
        JSONObject result = discInfosSpider.fetchDisc(asin);
        if (!result.getBoolean("success")) {
            return result.toString();
        }

        JSONObject discJson =  result.getJSONObject("data");
        if (discJson.getBoolean("offTheShelf")) {
            return errorMessage("可能该碟片已下架");
        }

        Disc disc = createDisc(asin, discJson);
        jmsHelper.sendDiscTrack(disc.getAsin(), disc.getTitle());

        JSONObject data = disc.toJSON();
        if (LOGGER.isInfoEnabled()) {
            infoRequest("[申请查询碟片][成功从日亚查询][ASIN={}][JSON={}]", asin, data);
        }
        return objectResult(data);
    }

    private Disc createDisc(@PathVariable String asin, JSONObject discJson) {
        Disc disc = new Disc(
            asin,
            createTitle(discJson),
            createType(discJson),
            createDate(discJson));
        dao.save(disc);
        return disc;
    }

    private String createTitle(JSONObject discJson) {
        return discJson.getString("title");
    }

    private DiscType createType(JSONObject discJson) {
        return DiscType.valueOf(discJson.getString("type"));
    }

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private LocalDate createDate(JSONObject discJson) {
        if (discJson.has("date")) {
            String dateString = discJson.getString("date");
            return LocalDate.parse(dateString, formatter);
        } else {
            return LocalDate.now();
        }
    }

}
