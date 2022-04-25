package com.mingzuozhibi.modules.connect;

import com.mingzuozhibi.commons.domain.Result;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class Connect {

    @Autowired
    private ConnectService connectService;

    public Result<String> waitResult(Module module, String uri) {
        String moduleName = module.getModuleName();
        Optional<String> host = connectService.getHost(moduleName);
        if (!host.isPresent()) {
            return Result.ofError(moduleName + "服务不可用");
        }
        try {
            String body = Jsoup.connect(host.get().concat(uri))
                .ignoreContentType(true)
                .execute()
                .body();
            return Result.ofData(body);
        } catch (Exception e) {
            return Result.ofError(e.toString());
        }
    }

}
