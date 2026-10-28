package modern.scheduler;

import modern.scheduler.core.Scheduler;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.StringReader;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 插件化任务调度器的入口。
 *   ./run.sh scheduler.SchedulerApp [配置文件] [运行秒数]
 * 不给配置文件时使用内置的演示配置，并自动生成一个插件目录（里面是 disk-report 插件）。
 */
public final class SchedulerApp {

    static final String DEMO_CONFIG = """
            scheduler.threads = 3
            scheduler.queue = 4
            scheduler.shutdownGrace = 2s
            scheduler.interceptors = logging, retry, timeout
            retry.maxAttempts = 3
            retry.backoff = 100ms
            timeout = 800ms

            jobs = hello, flaky, slow, stuck, disk

            # 每秒打个招呼
            job.hello.type = echo
            job.hello.schedule = every 1s
            job.hello.param.message = 你好，调度器

            # 下游不稳定：前两次尝试失败，靠重试拦截器救回来
            job.flaky.type = flaky
            job.flaky.schedule = every 2s
            job.flaky.param.failTimes = 2

            # 执行时间（700ms）比周期（300ms）长：不重叠，跳过
            job.slow.type = slow
            job.slow.schedule = every 300ms
            job.slow.param.sleep = 700

            # 卡住的任务：每次尝试都超时，重试三次后失败
            job.stuck.type = slow
            job.stuck.schedule = once
            job.stuck.param.sleep = 5000

            # 插件提供的任务类型
            job.disk.type = disk-report
            job.disk.schedule = once
            job.disk.param.path = .
            """;

    public static void main(String[] args) throws Exception {
        var config = new Properties();
        if (args.length > 0 && !args[0].matches("\\d+")) {
            try (var in = Files.newBufferedReader(Path.of(args[0]))) {
                config.load(in);
            }
        } else {
            config.load(new StringReader(DEMO_CONFIG));
            config.setProperty("plugins.dir", DemoPlugin.build().toString());
        }
        int seconds = Integer.parseInt(args.length > 1 ? args[1] : args.length == 1 && args[0].matches("\\d+") ? args[0] : "4");

        var assembler = new Assembler();
        Scheduler scheduler = assembler.assemble(config);
        scheduler.addLifecycleListener(e -> System.out.println("[lifecycle] " + e.source().getName() + " " + e.from() + " → " + e.to()));
        // 关闭钩子只调用 stop()：清理逻辑只有一份，正常退出和 Ctrl+C 走同一条路（第 16 章）
        Runtime.getRuntime().addShutdownHook(new Thread(scheduler::stop, "scheduler-shutdown-hook"));

        scheduler.start();
        Thread.sleep(seconds * 1000L);

        System.out.println("\n—— 运行 " + seconds + " 秒后，通过 JMX 读取统计（第 20 章）——");
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        for (ObjectName on : new java.util.TreeSet<>(mbs.queryNames(new ObjectName("modern.scheduler:type=Job,*"), null))) {
            System.out.printf("  %-6s type=%-12s %-14s runs=%s failures=%s skipped=%s last=%sms%n", on.getKeyProperty("name"),
                    mbs.getAttribute(on, "Type"), mbs.getAttribute(on, "Schedule"), mbs.getAttribute(on, "RunCount"),
                    mbs.getAttribute(on, "FailureCount"), mbs.getAttribute(on, "SkippedCount"), mbs.getAttribute(on, "LastDurationMillis"));
        }

        System.out.println("\n—— 停止 ——");
        scheduler.stop();
        assembler.close();
    }
}
