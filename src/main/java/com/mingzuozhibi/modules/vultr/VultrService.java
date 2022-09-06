package com.mingzuozhibi.modules.vultr;

import com.google.gson.*;
import com.mingzuozhibi.commons.base.BaseController;
import com.mingzuozhibi.commons.logger.LoggerBind;
import com.mingzuozhibi.commons.utils.MyTimeUtils;
import com.mingzuozhibi.commons.utils.ThreadUtils;
import com.mingzuozhibi.modules.disc.GroupService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.mingzuozhibi.commons.base.BaseKeys.*;
import static com.mingzuozhibi.commons.utils.FormatUtils.fmtDateTime2;
import static com.mingzuozhibi.commons.utils.ThreadUtils.runWithDaemon;

@Slf4j
@Service
@LoggerBind(Name.SERVER_CORE)
public class VultrService extends BaseController {

    private static final String TARGET = "BCloud";

    @Value("${bcloud.apikey}")
    private String vultrApiKey;

    @Autowired
    private GroupService groupService;

    @Autowired
    private VultrContext vultrContext;

    @PostConstruct
    public void init() {
        vultrContext.init();
        log.info("Vultr Instance Region = %s".formatted(vultrContext.formatRegion()));
        log.info("Vultr Instance Startted = %b".formatted(vultrContext.isStartted()));
        if (!vultrContext.isStartted() && vultrContext.getTimeout() != null) {
            checkServer(vultrContext.getTimeout());
        }
    }

    public void setStartted(boolean startted) {
        vultrContext.setStartted(startted);
    }

    private void checkServer(Instant timeout) {
        log.info("Vultr Instance Timeout = %s".formatted(
            MyTimeUtils.ofInstant(timeout).format(fmtDateTime2)
        ));
        runWithDaemon(bind, "检查服务器超时", () -> {
            while (true) {
                var millis = timeout.toEpochMilli() - Instant.now().toEpochMilli();
                if (millis <= 0) {
                    if (vultrContext.isStartted()) {
                        bind.success("服务器正常运行中");
                    } else {
                        bind.warning("服务器似乎已超时，重新开始任务");
                        tryRedoTask();
                    }
                    break;
                }
                ThreadUtils.sleepMillis(millis);
            }
        });
    }

    public void createServer() {
        List<TaskOfContent> tasks = groupService.findNeedUpdateDiscs().stream()
            .map(disc -> new TaskOfContent(disc.getAsin(), disc.getThisRank()))
            .collect(Collectors.toList());

        if (createInstance()) {
            vultrContext.setTaskCount(tasks.size());
            vultrContext.setDoneCount(0);
            vultrContext.setStartted(false);
            vultrContext.setTimeout(Instant.now().plus(15, ChronoUnit.MINUTES));
            checkServer(vultrContext.getTimeout());
            amqpSender.send(FETCH_TASK_START, gson.toJson(tasks));
            bind.info("JMS -> %s size=%d".formatted(FETCH_TASK_START, tasks.size()));
        }
    }

    public void finishServer(int doneCount) {
        vultrContext.setDoneCount(doneCount);
        var taskCount = vultrContext.getTaskCount();
        var skipCount = taskCount - doneCount;
        if (skipCount <= 100) {
            bind.success("更新日亚排名成功");
            deleteInstance();
        } else {
            bind.warning("更新日亚排名失败");
            Optional.ofNullable(vultrContext.getTimeout()).ifPresent(timeout -> {
                if (Instant.now().isBefore(timeout)) {
                    vultrContext.setStartted(false);
                } else {
                    tryRedoTask();
                }
            });
        }
    }

    private void tryRedoTask() {
        if (deleteInstance()) {
            waitForDelete();
            createServer();
        } else {
            bind.warning("未能重新开始任务");
        }
    }

    private void waitForDelete() {
        ThreadUtils.sleepSeconds(60);
        bind.debug("等待60秒以重新开始任务");
    }

    public boolean deleteInstance() {
        try {
            bind.notify("开始删除服务器");

            bind.debug("正在获取实例ID");
            Optional<JsonObject> instance = getInstance();
            if (instance.isEmpty()) {
                bind.warning("未能获取实例ID");
                return false;
            }

            String instanceId = instance.get().get("id").getAsString();
            String url = "https://api.vultr.com/v2/instances/%s".formatted(instanceId);
            Response response = jsoup(url, connection -> connection.method(Method.DELETE));
            if (response.statusCode() == 204) {
                bind.success("删除服务器成功");
                vultrContext.printStatus(instance.get());
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

    private boolean createInstance() {
        try {
            bind.notify("开始创建服务器");

            printVultrApiKey();

            bind.debug("正在检查服务器");
            Optional<JsonObject> instance = getInstance();
            if (instance.isPresent()) {
                bind.debug("检查到服务器已存在");
                if (deleteInstance()) {
                    waitForDelete();
                } else {
                    return false;
                }
            }

            bind.debug("正在获取快照ID");
            Optional<String> snapshotId = getSnapshotId();
            if (snapshotId.isEmpty()) {
                bind.warning("未能获取快照ID");
                return false;
            }

            bind.debug("正在获取防火墙策略ID");
            Optional<String> firewallId = getFirewallId();
            if (firewallId.isEmpty()) {
                bind.warning("未能获取防火墙策略ID");
                return false;
            }

            JsonObject payload = new JsonObject();
            payload.addProperty("region", vultrContext.useCode());
            payload.addProperty("plan", "vc2-1c-1gb");
            payload.addProperty("snapshot_id", snapshotId.get());
            payload.addProperty("backups", "disabled");
            payload.addProperty("enable_ipv6", false);
            payload.addProperty("firewall_group_id", firewallId.get());
            payload.addProperty("label", TARGET);
            payload.addProperty("hostname", TARGET);
            String body = payload.toString();
            bind.debug("服务器参数 = %s".formatted(body));

            bind.debug("Next Vultr Instance Region = %s".formatted(vultrContext.formatRegion()));

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
        log.debug("bcloud.apikey=%s**%s, length=%d".formatted(prefix, suffix, keylen));
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
                ThreadUtils.sleepSeconds(3, 5);
            }
        }
        throw lastThrow;
    }

}
