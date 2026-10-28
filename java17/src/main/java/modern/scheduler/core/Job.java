package modern.scheduler.core;

import modern.design.kit.LifecycleBase;
import modern.scheduler.spi.Task;

import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 一个任务（相当于 Wrapper：管理一个 Servlet 实例的生命周期）。
 * 它自己不调度、不执行，只保存「是什么、多久一次、跑得怎么样」；执行由 Scheduler 驱动。
 */
public final class Job extends LifecycleBase implements JobMXBean {

    private final String type;
    private final Task task;
    private final Schedule schedule;
    private final Map<String, String> params;
    private Scheduler scheduler;

    final AtomicBoolean running = new AtomicBoolean();      // 防止同一个任务重叠执行
    private final AtomicBoolean paused = new AtomicBoolean();
    final AtomicLong runs = new AtomicLong();
    final AtomicLong failures = new AtomicLong();
    final AtomicLong skipped = new AtomicLong();
    final AtomicLong lastDuration = new AtomicLong(-1);
    private ObjectName objectName;

    public Job(String name, String type, Task task, Schedule schedule, Map<String, String> params) {
        super(name);
        this.type = type;
        this.task = task;
        this.schedule = schedule;
        this.params = Map.copyOf(params);
    }

    void attach(Scheduler scheduler) { this.scheduler = scheduler; }
    Task task() { return task; }
    Schedule schedule() { return schedule; }
    Map<String, String> params() { return params; }

    @Override
    protected void startInternal() {
        try {                                                // 可观测性：启动时注册 MBean，停止时注销（资源对称，第 28 章）
            objectName = new ObjectName("modern.scheduler:type=Job,name=" + getName());
            ManagementFactory.getPlatformMBeanServer().registerMBean(this, objectName);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        scheduler.scheduleJob(this);
    }

    @Override
    protected void stopInternal() {
        try {
            ManagementFactory.getPlatformMBeanServer().unregisterMBean(objectName);
        } catch (Exception ignored) {
            // 已注销或从未注册：忽略
        }
    }

    /* ---------- JMX ---------- */
    @Override public String getType() { return type; }
    @Override public String getSchedule() { return schedule.once() ? "once" : "every " + schedule.period().toMillis() + "ms"; }
    @Override public long getRunCount() { return runs.get(); }
    @Override public long getFailureCount() { return failures.get(); }
    @Override public long getSkippedCount() { return skipped.get(); }
    @Override public long getLastDurationMillis() { return lastDuration.get(); }
    @Override public boolean isPaused() { return paused.get(); }
    @Override public void pause() { paused.set(true); }
    @Override public void resume() { paused.set(false); }
    @Override public void runNow() { scheduler.trigger(this); }
}
