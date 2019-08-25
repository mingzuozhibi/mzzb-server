package mingzuozhibi.action;

import mingzuozhibi.persist.disc.DiscInfo;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Transactional
    @GetMapping(value = "/api/newdiscs2", produces = MEDIA_TYPE)
    public String findAll2(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "20") int pageSize) {
        // 校验
        if (pageSize > 20 && noneMatchBasicRole()) {
            return errorMessage("设置pageSize大于20需要更多权限");
        }

        // dataObj
        @SuppressWarnings("unchecked")
        List<DiscInfo> discInfos = dao.create(DiscInfo.class)
                .addOrder(Order.desc("id"))
                .setFirstResult((page - 1) * pageSize)
                .setMaxResults(pageSize)
                .list();

        JSONArray dataObj = new JSONArray();
        discInfos.forEach(discInfo -> {
            dataObj.put(discInfo.toJSON());
        });

        // pageObj
        Long totalElements = (Long) dao.create(DiscInfo.class)
                .setProjection(Projections.rowCount())
                .uniqueResult();

        JSONObject pageObj = new JSONObject();
        pageObj.put("pageSize", pageSize);
        pageObj.put("currentPage", page);
        pageObj.put("totalElements", totalElements);

        return objectResult(dataObj, pageObj);
    }

    private boolean noneMatchBasicRole() {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .noneMatch("ROLE_BASIC"::equals);
    }

}
