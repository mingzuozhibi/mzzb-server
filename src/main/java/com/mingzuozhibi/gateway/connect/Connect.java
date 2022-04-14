package com.mingzuozhibi.gateway.connect;

import com.mingzuozhibi.commons.model.Result;
import com.mingzuozhibi.gateway.modules.Module;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class Connect {

    @Autowired
    private ConnectService connectService;

    public Result<String> waitRequest(Module module, String uri) {
        String moduleName = module.getModuleName();
        Optional<String> httpPrefix = connectService.getHttpPrefix(moduleName);
        return httpPrefix.map(s -> waitRequest(s + uri))
            .orElseGet(() -> Result.ofErrorMessage(moduleName + "服务不可用"));
    }

    private static Result<String> waitRequest(String url) {
        Result<String> result = new Result<>();
        try {
            String body = Jsoup.connect(url)
                .ignoreContentType(true)
                .execute()
                .body();
            result.setContent(body);
        } catch (Exception e) {
            result.pushError(e);
        }
        return result;
    }

}
