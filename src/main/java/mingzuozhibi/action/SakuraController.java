package mingzuozhibi.action;

import mingzuozhibi.persist.model.disc.Disc;
import mingzuozhibi.persist.model.discList.DiscList;
import mingzuozhibi.persist.model.discList.DiscListRepository;
import mingzuozhibi.persist.model.discSakura.DiscSakura;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SakuraController extends BaseController {


    @Autowired
    private DiscListRepository discListRepository;

    @GetMapping(value = "/api/sakura", produces = CONTENT_TYPE)
    public String sakura() {
        JSONArray data = new JSONArray();
        discListRepository.findBySakura(true)
                .forEach(discList -> data.add(buildDiscList(discList)));
        logger.debug("获取sakura数据, data.size=" + data.size());
        return objectResult(data);
    }

    private JSONObject buildDiscList(DiscList discList) {
        JSONObject object = new JSONObject();
        object.put("name", discList.getName());
        object.put("title", discList.getTitle());
        if (discList.getDate() != null)
            object.put("update_date", discList.getDate().getTime());
        object.put("discs", buildDiscs(discList.getDiscs()));
        return object;
    }

    private JSONArray buildDiscs(List<Disc> discs) {
        JSONArray array = new JSONArray();
        discs.forEach(disc -> {
            JSONObject object = new JSONObject();
            object.put("id", disc.getId());
            object.put("asin", disc.getAsin());
            object.put("japan", disc.getJapan());
            object.put("title", disc.getTitle());
            if (disc.getSname() != null)
                object.put("sname", disc.getSname());
            else
                object.put("sname", disc.getTitle());
            object.put("type", disc.getType().name());
            DiscSakura sakura = disc.getSakura();
            if (sakura != null) {
                object.put("this_rank", sakura.getCurk());
                object.put("prev_rank", sakura.getPrrk());
                object.put("this_book", sakura.getCubk());
                object.put("total_point", sakura.getCupt());
                object.put("surplus_days", sakura.getSday());
                if (sakura.getDate() != null)
                    object.put("update_time", sakura.getDate().getTime());
            }
            array.add(object);
        });
        return array;
    }

}
