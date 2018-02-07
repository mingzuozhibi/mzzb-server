package mingzuozhibi.action;

import mingzuozhibi.persist.Sakura;
import mingzuozhibi.support.Dao;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class SakuraController extends BaseController {

    @Autowired
    private Dao dao;

    @GetMapping(value = "/api/sakura", produces = CONTENT_TYPE)
    public String sakura(@RequestParam("discColumns") String discColumns) {
        JSONArray data = new JSONArray();
        List<Sakura> sakuras = dao.findBy(Sakura.class, "enabled", true);
        LOGGER.debug("获取sakura数据, 共{}个列表, 请求参数: {}", sakuras.size(), discColumns);
        Set<String> columns = Arrays.stream(discColumns.split(",")).collect(Collectors.toSet());
        sakuras.forEach(sakura -> {
            data.put(sakura.toJSON(true, columns));
            LOGGER.debug("列表[{}]共{}个碟片", sakura.getTitle(), sakura.getDiscs().size());
        });
        return objectResult(data);
    }

}
