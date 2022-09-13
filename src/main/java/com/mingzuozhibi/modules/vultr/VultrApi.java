package com.mingzuozhibi.modules.vultr;

import com.google.gson.JsonObject;
import com.mingzuozhibi.commons.base.BaseKeys.Name;
import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.commons.logger.LoggerBind;
import com.mingzuozhibi.modules.vultr.sdk.VultrJsoup;
import org.jsoup.Connection.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Objects;
import java.util.Optional;

import static com.mingzuozhibi.modules.vultr.sdk.VultrJsoup.*;

@Component
@LoggerBind(Name.SERVER_CORE)
public class VultrApi extends BaseSupport {

    private static final String TARGET = "BCloud";

    @Value("${bcloud.apikey}")
    private String apiKey;

    @PostConstruct
    public void init() {
        VultrJsoup.init(bind, apiKey);
    }

    public boolean createInstance(String code, String snapshotId, String firewallId) throws Exception {
        var payload = new JsonObject();
        payload.addProperty("region", code);
        payload.addProperty("plan", "vc2-1c-1gb");
        payload.addProperty("snapshot_id", snapshotId);
        payload.addProperty("backups", "disabled");
        payload.addProperty("enable_ipv6", false);
        payload.addProperty("firewall_group_id", firewallId);
        payload.addProperty("label", TARGET);
        payload.addProperty("hostname", TARGET);
        var body = payload.toString();
        bind.debug("服务器参数 = %s".formatted(body));

        var response = jsoupPost("https://api.vultr.com/v2/instances", body);
        if (response.statusCode() == 202) {
            bind.success("创建服务器成功");
            return true;
        } else {
            bind.warning("创建服务器失败：%s".formatted(response.statusMessage()));
            return false;
        }
    }

    public boolean deleteInstance(String instanceId) throws Exception {
        var url = "https://api.vultr.com/v2/instances/%s".formatted(instanceId);
        var response = jsoup(url, connection -> connection.method(Method.DELETE));
        if (response.statusCode() == 204) {
            bind.success("删除服务器成功");
            return true;
        } else {
            bind.warning("删除服务器失败：%s".formatted(response.statusMessage()));
            return false;
        }
    }

    public Optional<JsonObject> getInstance() throws Exception {
        var body = jsoupGet("https://api.vultr.com/v2/instances");
        var root = gson.fromJson(body, JsonObject.class);
        var instances = root.get("instances").getAsJsonArray();
        for (var e : instances) {
            var instance = e.getAsJsonObject();
            if (Objects.equals(instance.get("label").getAsString(), TARGET)) {
                return Optional.of(instance);
            }
        }
        return Optional.empty();
    }

    public Optional<String> getSnapshotId() throws Exception {
        var body = jsoupGet("https://api.vultr.com/v2/snapshots");
        var root = gson.fromJson(body, JsonObject.class);
        var snapshots = root.get("snapshots").getAsJsonArray();
        for (var e : snapshots) {
            var snapshot = e.getAsJsonObject();
            if (Objects.equals(snapshot.get("description").getAsString(), TARGET)) {
                return Optional.ofNullable(snapshot.get("id").getAsString());
            }
        }
        return Optional.empty();
    }

    public Optional<String> getFirewallId() throws Exception {
        var body = jsoupGet("https://api.vultr.com/v2/firewalls");
        var root = gson.fromJson(body, JsonObject.class);
        var firewalls = root.get("firewall_groups").getAsJsonArray();
        for (var e : firewalls) {
            var firewall = e.getAsJsonObject();
            if (Objects.equals(firewall.get("description").getAsString(), TARGET)) {
                return Optional.ofNullable(firewall.get("id").getAsString());
            }
        }
        return Optional.empty();
    }

}
