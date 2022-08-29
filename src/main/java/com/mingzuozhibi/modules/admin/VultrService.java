package com.mingzuozhibi.modules.admin;

import com.google.gson.*;
import com.mingzuozhibi.commons.amqp.AmqpEnums.Name;
import com.mingzuozhibi.commons.amqp.logger.LoggerBind;
import com.mingzuozhibi.commons.base.BaseController;
import com.mingzuozhibi.commons.utils.ThreadUtils;
import com.mingzuozhibi.modules.core.VarableService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static com.mingzuozhibi.commons.utils.FormatUtils.fmtDateTime2;
import static com.mingzuozhibi.support.FileIoUtils.writeLine;

@Slf4j
@Service
@LoggerBind(Name.SERVER_CORE)
public class VultrService extends BaseController {

    private static final String[] REGIONS =
        {
            "atl", "dfw", "ewr", "lax", "mia", "ord", "sea", "sjc", "mex", "yto",
            "ams", "cdg", "fra", "lhr", "sto", "waw", "mel", "syd", "sgp", "nrt"
        };

    private static final String REGION_INDEX = "VultrService.regionIndex";
    private static final String TASK_COUNT = "VultrService.taskCount";
    private static final String DONE_COUNT = "VultrService.doneCount";
    private static final String TARGET = "BCloud";

    @Value("${bcloud.apikey}")
    private String vultrApiKey;

    @Autowired
    private VarableService varableService;

    private int regionIndex;

    private int taskCount;

    private int doneCount;

    public void setRegionIndex(int regionIndex) {
        this.regionIndex = regionIndex % REGIONS.length;
        varableService.saveOrUpdate(REGION_INDEX, "%d".formatted(this.regionIndex));
    }

    public void setTaskCount(int taskCount) {
        this.taskCount = taskCount;
        varableService.saveOrUpdate(TASK_COUNT, "%d".formatted(this.taskCount));
    }

    public void setDoneCount(int doneCount) {
        this.doneCount = doneCount;
        varableService.saveOrUpdate(DONE_COUNT, "%d".formatted(this.doneCount));
    }

    @PostConstruct
    public void init() {
        varableService.findIntegerByKey(REGION_INDEX)
            .ifPresent(this::setRegionIndex);
        varableService.findIntegerByKey(TASK_COUNT)
            .ifPresent(this::setTaskCount);
        varableService.findIntegerByKey(DONE_COUNT)
            .ifPresent(this::setDoneCount);
        log.info("Vultr Instance Region = %s".formatted(formatRegion()));
    }

    private String formatRegion() {
        return "%d (%s)".formatted(regionIndex, REGIONS[regionIndex]);
    }

    public boolean deleteInstance() {
        try {
            bind.notify("开始删除服务器");

            bind.info("正在获取实例ID");
            Optional<JsonObject> instance = getInstance();
            if (instance.isEmpty()) {
                bind.info("未能获取实例ID");
                return false;
            }

            printRegionMainIp(instance.get());

            String instanceId = instance.get().get("id").getAsString();
            String url = "https://api.vultr.com/v2/instances/%s".formatted(instanceId);
            Response response = jsoup(url, connection -> connection.method(Method.DELETE));
            if (response.statusCode() == 204) {
                bind.success("删除服务器成功");
                return true;
            } else {
                bind.warning("删除服务器失败：%s".formatted(response.statusMessage()));
                return false;
            }
        } catch (Exception e) {
            bind.error("删除服务器异常：%s".formatted(e));
            return false;
        }
    }

    public boolean createInstance() {
        try {
            bind.notify("开始创建服务器");

            printVultrApiKey();

            bind.info("正在检查服务器");
            Optional<JsonObject> instance = getInstance();
            if (instance.isPresent()) {
                bind.info("检查到服务器已存在");
                if (deleteInstance()) {
                    ThreadUtils.threadSleep(60);
                } else {
                    return false;
                }
            }

            bind.info("正在获取快照ID");
            Optional<String> snapshotId = getSnapshotId();
            if (snapshotId.isEmpty()) {
                bind.info("未能获取快照ID");
                return false;
            }

            bind.info("正在获取防火墙策略ID");
            Optional<String> firewallId = getFirewallId();
            if (firewallId.isEmpty()) {
                bind.info("未能获取防火墙策略ID");
                return false;
            }

            JsonObject payload = new JsonObject();
            payload.addProperty("region", REGIONS[regionIndex]);
            payload.addProperty("plan", "vc2-1c-1gb");
            payload.addProperty("snapshot_id", snapshotId.get());
            payload.addProperty("backups", "disabled");
            payload.addProperty("enable_ipv6", false);
            payload.addProperty("firewall_group_id", firewallId.get());
            payload.addProperty("label", TARGET);
            payload.addProperty("hostname", TARGET);
            String body = payload.toString();
            bind.info("服务器参数 = %s".formatted(body));

            setRegionIndex(regionIndex + 1);
            bind.info("Next Vultr Instance Region = %s".formatted(formatRegion()));

            Response response = jsoupPost("https://api.vultr.com/v2/instances", body);
            if (response.statusCode() == 202) {
                bind.success("创建服务器成功");
                return true;
            } else {
                bind.warning("创建服务器失败：%s".formatted(response.statusMessage()));
                return false;
            }
        } catch (Exception e) {
            bind.error("创建服务器异常：%s".formatted(e));
            return false;
        }
    }

    private void printVultrApiKey() {
        var keylen = vultrApiKey.length();
        var prefix = vultrApiKey.substring(0, 2);
        var suffix = vultrApiKey.substring(keylen - 2);
        log.info("bcloud.apikey=%s**%s, length=%d".formatted(prefix, suffix, keylen));
    }

    private void printRegionMainIp(JsonObject instance) {
        var mainIp = instance.get("main_ip").getAsString();
        var region = instance.get("region").getAsString();
        var nextCount = taskCount - doneCount;
        var status = "mainIp = %s, region = %d(%s), task = %d, done = %d, next = %d".formatted(
            mainIp, regionIndex, region, taskCount, doneCount, nextCount
        );

        if (nextCount > 25 && taskCount > 100) {
            log.warn("Instance Status: %s".formatted(status));
            bind.warning("抓取状态异常：%s".formatted(status));
        } else {
            log.info("Instance Status: %s".formatted(status));
        }

        var datetime = LocalDateTime.now().format(fmtDateTime2);
        writeLine("var/instance.log", "[%s] %s".formatted(datetime, status));
    }

    public Optional<JsonObject> getInstance() throws Exception {
        String body = jsoupGet("https://api.vultr.com/v2/instances");
        JsonObject root = gson.fromJson(body, JsonObject.class);
        JsonArray instances = root.get("instances").getAsJsonArray();
        for (JsonElement e : instances) {
            JsonObject instance = e.getAsJsonObject();
            if (Objects.equals(instance.get("label").getAsString(), TARGET)) {
                return Optional.of(instance);
            }
        }
        return Optional.empty();
    }

    private Optional<String> getSnapshotId() throws Exception {
        String body = jsoupGet("https://api.vultr.com/v2/snapshots");
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

    private Optional<String> getFirewallId() throws Exception {
        String body = jsoupGet("https://api.vultr.com/v2/firewalls");
        JsonObject root = gson.fromJson(body, JsonObject.class);
        JsonArray firewalls = root.get("firewall_groups").getAsJsonArray();
        for (JsonElement e : firewalls) {
            JsonObject firewall = e.getAsJsonObject();
            if (Objects.equals(firewall.get("description").getAsString(), TARGET)) {
                return Optional.ofNullable(firewall.get("id").getAsString());
            }
        }
        return Optional.empty();
    }

    private Response jsoupPost(String url, String body) throws Exception {
        return jsoup(url, connection -> connection.method(Method.POST).requestBody(body));
    }

    private String jsoupGet(String url) throws Exception {
        return jsoup(url, connection -> connection.method(Method.GET)).body();
    }

    private Response jsoup(String url, Consumer<Connection> consumer) throws Exception {
        Exception lastThrow = null;
        int maxCount = 8;
        for (int i = 0; i < maxCount; i++) {
            try {
                Connection connection = Jsoup.connect(url)
                    .header("Authorization", "Bearer %s".formatted(vultrApiKey))
                    .header("Content-Type", "application/json")
                    .timeout(10000)
                    .ignoreContentType(true);
                consumer.accept(connection);
                return connection.execute();
            } catch (Exception e) {
                lastThrow = e;
                bind.debug("jsoup(%s) throws %s (%d/%d)".formatted(url, e, i + 1, maxCount));
                ThreadUtils.threadSleep(3, 5);
            }
        }
        throw lastThrow;
    }

}
