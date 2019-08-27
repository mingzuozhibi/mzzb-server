package mingzuozhibi.action;

import mingzuozhibi.persist.disc.Sakura;
import org.hibernate.Criteria;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collector;

import static org.hibernate.criterion.Restrictions.ne;

@RestController
public class DiscGroupController extends BaseController {

    @Deprecated
    @Transactional
    @GetMapping(value = "/api/sakuras", produces = MEDIA_TYPE)
    public String findAllDeprecated(@RequestParam(name = "public", defaultValue = "true") boolean isPublic) {
        Criteria criteria = dao.session().createCriteria(Sakura.class);
        if (isPublic) {
            criteria.add(ne("viewType", ViewType.PrivateList));
        }

        @SuppressWarnings("unchecked")
        List<Sakura> discGroups = criteria.list();

        JSONArray array = discGroups.stream()
                .map(this::toJSONDeprecated)
                .collect(toJSONArray());
        return objectResult(array);
    }

    @Transactional
    @GetMapping(value = "/api/discGroups", produces = MEDIA_TYPE)
    public String findAll(@RequestParam(defaultValue = "false") boolean hasPrivate) {
        Criteria criteria = dao.session().createCriteria(Sakura.class);
        if (!hasPrivate) {
            criteria.add(ne("viewType", Sakura.ViewType.PrivateList));
        }

        @SuppressWarnings("unchecked")
        List<Sakura> discGroups = criteria.list();

        JSONArray array = discGroups.stream()
                .map(this::toJSON)
                .collect(toJSONArray());
        return objectResult(array);
    }

    @Deprecated
    private JSONObject toJSONDeprecated(Sakura sakura) {
        return sakura.toJSON().put("discsSize", sakura.getDiscs().size());
    }

    private JSONObject toJSON(Sakura sakura) {
        return sakura.toJSON().put("discCount", sakura.getDiscs().size());
    }

    private Collector<JSONObject, JSONArray, JSONArray> toJSONArray() {
        return Collector.of(JSONArray::new, JSONArray::put, (objects, objects2) -> {
            return objects.put(objects2.toList());
        });
    }

}
