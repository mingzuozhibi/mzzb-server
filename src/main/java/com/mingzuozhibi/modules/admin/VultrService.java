package com.mingzuozhibi.modules.admin;

import com.google.gson.*;
import com.mingzuozhibi.commons.amqp.AmqpEnums.Name;
import com.mingzuozhibi.commons.amqp.logger.LoggerBind;
import com.mingzuozhibi.commons.base.BaseController;
import com.mingzuozhibi.commons.domain.Result;
import com.mingzuozhibi.commons.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@LoggerBind(Name.SERVER_CORE)
public class VultrService extends BaseController {

    private static final String TARGET = "BCloud";
    private static final String[] REGIONS = {"atl", "dfw", "ewr", "lax", "mia", "ord", "sea", "sjc"};

    @Value("${vultr.api.key}")
    private String vultrApiKey;

    private int regionIndex;

    {
        regionIndex = new Random().nextInt(REGIONS.length);
        log.info("Region = %d (%s)".formatted(regionIndex, REGIONS[regionIndex]));
    }

    public Result<String> deleteInstance() {
        try {
            bind.notify("开始删除服务器");

            bind.info("正在获取实例ID");
            Optional<String> instanceId = getInstanceId();
            if (instanceId.isEmpty()) {
                bind.info("未能获取实例ID");
                return Result.ofError("Can not find instance " + TARGET);
            }

            Response response = jsoup("https://api.vultr.com/v2/instances/" + instanceId.get())
                .method(Method.DELETE)
                .execute();
            if (response.statusCode() == 204) {
                bind.success("删除服务器成功");
                return Result.ofData("Delete instance success");
            } else {
                bind.warning("删除服务器失败：" + response.statusMessage());
                return Result.ofError("Delete instance error: " + response.statusMessage());
            }
        } catch (IOException e) {
            bind.error("删除服务器异常：" + e);
            return Result.ofError(e.toString());
        }
    }

    public Result<String> createInstance() {
        try {
            bind.notify("开始创建服务器");

            int keylen = vultrApiKey.length();
            String keystr = vultrApiKey.substring(0, 2) + "**" + vultrApiKey.substring(keylen - 2);
            log.info("vultr.api.key={}, length={}", keystr, keylen);

            bind.info("正在检查服务器");
            if (getInstanceId().isPresent()) {
                bind.info("检查到服务器已存在");
                Result<String> result = deleteInstance();
                if (result.isSuccess()) {
                    ThreadUtils.threadSleep(60);
                } else {
                    return Result.ofError("Instance can not delete: " + result.getMessage());
                }
            }

            bind.info("正在获取快照ID");
            Optional<String> snapshotId = getSnapshotId();
            if (snapshotId.isEmpty()) {
                bind.info("未能获取快照ID");
                return Result.ofError("Can not find snapshot " + TARGET);
            }

            bind.info("正在获取防火墙策略ID");
            Optional<String> firewallId = getFirewallId();
            if (firewallId.isEmpty()) {
                bind.info("未能获取防火墙策略ID");
                return Result.ofError("Can not find firewall " + TARGET);
            }

            JsonObject payload = new JsonObject();
            payload.addProperty("region", REGIONS[regionIndex++ % REGIONS.length]);
            payload.addProperty("plan", "vc2-1c-1gb");
            payload.addProperty("snapshot_id", snapshotId.get());
            payload.addProperty("backups", "disabled");
            payload.addProperty("enable_ipv6", false);
            payload.addProperty("firewall_group_id", firewallId.get());
            payload.addProperty("label", TARGET);
            payload.addProperty("hostname", TARGET);
            String payloadAsString = payload.getAsString();
            bind.info("服务器参数 = " + payloadAsString);

            Response response = jsoup("https://api.vultr.com/v2/instances")
                .requestBody(payloadAsString)
                .method(Method.POST)
                .execute();
            if (response.statusCode() == 202) {
                bind.success("创建服务器成功");
                return Result.ofData("Create instance success");
            } else {
                bind.warning("创建服务器失败：" + response.statusMessage());
                return Result.ofError("Create instance error: " + response.statusMessage());
            }
        } catch (IOException e) {
            bind.error("创建服务器异常：" + e);
            return Result.ofError(e.toString());
        }
    }

    private Optional<String> getInstanceId() throws IOException {
        String body = jsoup("https://api.vultr.com/v2/instances")
            .get().body().text();
        JsonObject root = gson.fromJson(body, JsonObject.class);
        JsonArray instances = root.get("instances").getAsJsonArray();
        for (JsonElement e : instances) {
            JsonObject instance = e.getAsJsonObject();
            if (Objects.equals(instance.get("label").getAsString(), TARGET)) {
                return Optional.ofNullable(instance.get("id").getAsString());
            }
        }
        return Optional.empty();
    }

    private Optional<String> getSnapshotId() throws IOException {
        String body = jsoup("https://api.vultr.com/v2/snapshots")
            .get().body().text();
        JsonObject root = gson.fromJson(body, JsonObject.class);
        JsonArray snapshots = root.get("snapshots").getAsJsonArray();
        for (JsonElement e : snapshots) {
            JsonObject snapshot = e.getAsJsonObject();
            if (Objects.equals(snapshot.get("description").getAsString(), TARGET)) {
                return Optional.ofNullable(snapshot.get("id").getAsString());
            }
        }
        return Optional.empty();
    }

    private Optional<String> getFirewallId() throws IOException {
        String body = jsoup("https://api.vultr.com/v2/firewalls")
            .get().body().text();
        JsonObject root = gson.fromJson(body, JsonObject.class);
        JsonArray firewalls = root.get("firewalls").getAsJsonArray();
        for (JsonElement e : firewalls) {
            JsonObject firewall = e.getAsJsonObject();
            if (Objects.equals(firewall.get("description").getAsString(), TARGET)) {
                return Optional.ofNullable(firewall.get("id").getAsString());
            }
        }
        return Optional.empty();
    }

    private Connection jsoup(String url) {
        return Jsoup.connect(url)
            .header("Authorization", "Bearer " + vultrApiKey)
            .header("Content-Type", "application/json")
            .ignoreContentType(true);
    }

}
