package mingzuozhibi.action;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
