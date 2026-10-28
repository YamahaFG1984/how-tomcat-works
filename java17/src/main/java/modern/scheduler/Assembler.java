package modern.scheduler;

import modern.design.kit.PluginClassLoader;
import modern.scheduler.core.Interceptors;
import modern.scheduler.core.Job;
import modern.scheduler.core.Schedule;
import modern.scheduler.core.Scheduler;
import modern.scheduler.spi.TaskType;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * 装配层（相当于 Digester + HostConfig）：配置 → 发现插件 → 创建对象 → 连成树。
 * 业务组件里没有任何 new 具体实现；所有「谁和谁连在一起」都在这里。
 *
 *   scheduler.threads = 2            scheduler.queue = 4           scheduler.shutdownGrace = 2s
 *   scheduler.interceptors = logging, retry, timeout
 *   retry.maxAttempts = 3            retry.backoff = 100ms         timeout = 1s
 *   plugins.dir = plugins
 *   jobs = hello, flaky, slow
 *   job.hello.type = echo            job.hello.schedule = every 1s job.hello.param.message = 你好
 */
public final class Assembler {

    private final List<PluginClassLoader> pluginLoaders = new ArrayList<>();

    public Scheduler assemble(Properties p) throws IOException {
        var scheduler = new Scheduler("scheduler",
                Integer.parseInt(p.getProperty("scheduler.threads", "2")),
                Integer.parseInt(p.getProperty("scheduler.queue", "8")),
                Schedule.parseDuration(p.getProperty("scheduler.shutdownGrace", "3s")));

        // 拦截器：顺序就是配置里写的顺序（外层在前）
        for (String name : list(p, "scheduler.interceptors")) {
            scheduler.pipeline().addValve(switch (name) {
                case "logging" -> Interceptors.logging();
                case "retry" -> Interceptors.retry(Integer.parseInt(p.getProperty("retry.maxAttempts", "3")),
                        Schedule.parseDuration(p.getProperty("retry.backoff", "100ms")), scheduler);
                case "timeout" -> Interceptors.timeout(Schedule.parseDuration(p.getProperty("timeout", "5s")), scheduler.watchdog());
                default -> throw new IllegalArgumentException("未知的拦截器：" + name);
            });
        }

        // 监听器：失败告警
        scheduler.addListener(e -> {
            switch (e.type()) {
                case RETRYING -> System.out.println("  [retry] 任务 " + e.job() + " 第 " + e.attempt() + " 次失败（" + e.error().getMessage() + "），稍后重试");
                case FAILED -> System.out.println("  [alert] 任务 " + e.job() + " 在第 " + e.attempt() + " 次尝试后失败：" + e.error().getMessage());
                case SKIPPED_OVERLAP -> System.out.println("  [alert] 任务 " + e.job() + " 上一次还没结束，本次跳过");
                case REJECTED -> System.out.println("  [alert] 线程池已满，任务 " + e.job() + " 被拒绝");
                default -> { }
            }
        });

        Map<String, TaskType> types = discover(p.getProperty("plugins.dir"));
        System.out.println("任务类型：" + new TreeSet<>(types.keySet()));

        for (String job : list(p, "jobs")) {
            String prefix = "job." + job + ".";
            String typeName = require(p, prefix + "type");
            TaskType type = types.get(typeName);
            if (type == null) throw new IllegalArgumentException("任务 " + job + " 的类型 " + typeName + " 不存在");  // 启动时就失败
            Map<String, String> params = new LinkedHashMap<>();
            for (String key : p.stringPropertyNames()) {
                if (key.startsWith(prefix + "param.")) params.put(key.substring((prefix + "param.").length()), p.getProperty(key));
            }
            scheduler.addChild(new Job(job, typeName, type.create(params), Schedule.parse(require(p, prefix + "schedule")), params));
        }
        return scheduler;
    }

    /** 类路径上的内置类型 + 插件目录下每个子目录（或 jar）各一个隔离的类加载器 */
    private Map<String, TaskType> discover(String pluginsDir) throws IOException {
        Map<String, TaskType> types = new LinkedHashMap<>();
        ClassLoader app = Assembler.class.getClassLoader();
        register(types, app, "classpath");
        if (pluginsDir != null && Files.isDirectory(Path.of(pluginsDir))) {
            try (Stream<Path> entries = Files.list(Path.of(pluginsDir))) {
                for (Path plugin : entries.sorted().toList()) {
                    var loader = new PluginClassLoader(plugin.getFileName().toString(), new URL[] {plugin.toUri().toURL()},
                            app, "modern.scheduler.spi.", "modern.design.kit.");
                    pluginLoaders.add(loader);
                    register(types, loader, "plugin " + plugin.getFileName());
                }
            }
        }
        return types;
    }

    private static void register(Map<String, TaskType> types, ClassLoader loader, String source) {
        ServiceLoader.load(TaskType.class, loader).stream()
                .filter(p -> p.type().getClassLoader() == loader)        // 只要这个加载器自己的（第 30 章踩过的坑）
                .map(ServiceLoader.Provider::get)
                .forEach(t -> {
                    if (types.putIfAbsent(t.name(), t) != null) throw new IllegalStateException("任务类型重名：" + t.name());
                    System.out.printf("  发现 %-12s ← %s（%s）%n", t.name(), source, t.description());
                });
    }

    /** 停止后关闭插件类加载器（资源对称） */
    public void close() throws IOException {
        for (var l : pluginLoaders) l.close();
    }

    private static String require(Properties p, String key) {
        String v = p.getProperty(key);
        if (v == null || v.isBlank()) throw new IllegalArgumentException("缺少配置 " + key);
        return v.trim();
    }

    private static List<String> list(Properties p, String key) {
        return Stream.of(p.getProperty(key, "").split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
