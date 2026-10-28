package modern.scheduler.core;

import modern.scheduler.spi.TaskContext;

import java.util.Map;

/**
 * 一次执行在拦截器链里流动的上下文（相当于 Request + Response）。
 * 对任务只暴露 TaskContext 视图（外观），拦截器可以看到更多。
 */
public final class Execution implements TaskContext {

    private final Job job;
    private int attempt = 1;
    private final long startNanos = System.nanoTime();

    Execution(Job job) {
        this.job = job;
    }

    public Job job() { return job; }
    public void nextAttempt() { attempt++; }
    public long elapsedMillis() { return (System.nanoTime() - startNanos) / 1_000_000; }

    @Override public String jobName() { return job.getName(); }
    @Override public Map<String, String> params() { return job.params(); }
    @Override public int attempt() { return attempt; }
    @Override public void log(String message) {
        System.out.printf("  [%s#%d %s] %s%n", jobName(), attempt, Thread.currentThread().getName(), message);
    }
}
