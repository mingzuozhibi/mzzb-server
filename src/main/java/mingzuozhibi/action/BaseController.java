package mingzuozhibi.action;

import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseController {

    protected static final String CONTENT_TYPE = "application/json;charset=UTF-8";
    protected Logger logger;

    public BaseController() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    protected String booleanResult(boolean success) {
        JSONObject root = new JSONObject();
        root.put("success", success);
        return root.toString();
    }

    protected String objectResult(Object object) {
        JSONObject root = new JSONObject();
        root.put("success", true);
        root.put("data", object);
        return root.toString();
    }

    protected String errorMessage(String message) {
        JSONObject root = new JSONObject();
        root.put("success", false);
        root.put("message", message);
        return root.toString();
    }

}
