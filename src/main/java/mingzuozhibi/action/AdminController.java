package mingzuozhibi.action;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static mingzuozhibi.utils.DiscUtils.needUpdateAsins;

@RestController
public class AdminController extends BaseController {

    @Transactional
    @GetMapping(value = "/api/admin/fetchCount")
    public String fetchCount() {
        return objectResult(needUpdateAsins(dao.session()).size());
    }

    @Deprecated
    @Transactional
    @GetMapping(value = "/api/discs/activeCount")
    public String deprecatedFetchCount() {
        return fetchCount();
    }

}
