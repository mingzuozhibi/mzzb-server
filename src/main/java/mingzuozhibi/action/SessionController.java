package mingzuozhibi.action;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController extends BaseController {

    @GetMapping(value = "/api/session", produces = MEDIA_TYPE)
    public String current() {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();

        JSONObject json = getJSON(authentication);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[{}][状态获取][json={}]",
                    getAttributes().getRequest().getRemoteAddr(), json);
        }
        return objectResult(json);
    }

    public static JSONObject getJSON(Authentication authentication) {
        JSONObject object = new JSONObject();

        if (authentication != null) {
            String name = authentication.getName();
            boolean isLogged = authentication.isAuthenticated() && !"anonymousUser".equals(name);
            if (isLogged) {
                object.put("userName", name);
                object.put("isLogged", true);
                object.put("userRoles", getUserRoles(authentication));
                return object;
            }
        }

        object.put("userName", "Guest");
        object.put("isLogged", false);
        object.put("userRoles", new JSONArray());
        return object;
    }

    public static JSONArray getUserRoles(Authentication authentication) {
        JSONArray userRoles = new JSONArray();
        authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .forEach(userRoles::put);
        return userRoles;
    }

}
