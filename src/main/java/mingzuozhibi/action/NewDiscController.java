package mingzuozhibi.action;

import mingzuozhibi.persist.disc.DiscInfo;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class NewDiscController extends BaseController {

    @Transactional
    @GetMapping(value = "/api/newdiscs", produces = MEDIA_TYPE)
    public String findAll(@RequestParam(defaultValue = "0") int page) {
        Long rowCount = (Long) dao.create(DiscInfo.class)
                .setProjection(Projections.rowCount())
                .uniqueResult();
        int maxSize = 20;
        int maxPage = (rowCount.intValue() - 1) / 20;

        if (page < 0) page = 0;
        if (page > maxPage) page = maxPage;

        @SuppressWarnings("unchecked")
        List<DiscInfo> discInfos = dao.create(DiscInfo.class)
                .addOrder(Order.desc("id"))
                .setFirstResult(page * maxSize)
                .setMaxResults(maxSize)
                .list();

        JSONObject result = new JSONObject();
        JSONArray newdiscs = new JSONArray();

        discInfos.forEach(discInfo -> {
            newdiscs.put(discInfo.toJSON());
        });

        result.put("newdiscs", newdiscs);

        JSONObject pageInfo = new JSONObject();
        pageInfo.put("page", page);
        pageInfo.put("size", newdiscs.length());
        pageInfo.put("maxPage", maxPage);
        pageInfo.put("maxSize", maxSize);

        result.put("pageInfo", pageInfo);
        return objectResult(result);
    }

}
