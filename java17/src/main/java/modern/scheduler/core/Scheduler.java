package modern.scheduler.core;

import modern.design.kit.CompositeComponent;
import modern.design.kit.Pipeline;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 调度器（相当于 Engine + Connector）：
 *   ticker  一个线程负责「到点」——只投递，不执行（第 23 章的 Poller、第 12 章的后台线程）
 *   workers 有界线程池负责执行；队列满了就拒绝并发事件，而不是无限堆积（第 23 章的三个上限）
 *   pipeline 每次执行都经过拦截器链，基础阀调用任务本身（第 5 章）
 */
public final class Scheduler extends CompositeComponent<Job> {

    private final int workerThreads;
    private final int queueCapacity;
    private final Duration shutdownGrace;
    private final List<Consumer<JobEvent>> listeners = new CopyOnWriteArrayList<>();
    private final Pipeline<Execution> pipeline = new Pipeline<>(ex -> ex.job().task().run(ex));

    private ScheduledExecutorService ticker;
    private ScheduledExecutorService watchdog;
    private ThreadPoolExecutor workers;

    public Scheduler(String name, int workerThreads, int queueCapacity, Duration shutdownGrace) {
        super(name);
        this.workerThreads = workerThreads;
        this.queueCapacity = queueCapacity;
        this.shutdownGrace = shutdownGrace;
    }

    public Pipeline<Execution> pipeline() { return pipeline; }
    public void addListener(Consumer<JobEvent> l) { listeners.add(l); }

    /** 看门狗定时器给超时拦截器用；装配时就需要，所以在构造后立刻可用 */
    public ScheduledExecutorService watchdog() {
        if (watchdog == null) watchdog = Executors.newSingleThreadScheduledExecutor(named("scheduler-watchdog"));
        return watchdog;
    }

    @Override
    public void addChild(Job job) {
        job.attach(this);
        super.addChild(job);
    }

    @Override
    protected void startSelf() {                                // 先准备好线程，子组件（Job）启动时才能注册调度
        ticker = Executors.newSingleThreadScheduledExecutor(named("scheduler-ticker"));
        workers = new ThreadPoolExecutor(workerThreads, workerThreads, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity), named("job-worker"), new ThreadPoolExecutor.AbortPolicy());
        watchdog();
    }

    @Override
    protected void stopSelf() {                                 // 子组件都停了之后：先停止投递，再等在途任务
        ticker.shutdownNow();
        workers.shutdown();
        try {
            if (!workers.awaitTermination(shutdownGrace.toMillis(), TimeUnit.MILLISECONDS)) {
                System.out.println("  [scheduler] " + shutdownGrace.toMillis() + " ms 内没有结束，中断剩余任务");
                workers.shutdownNow();
            }
        } catch (InterruptedException e) {
            workers.shutdownNow();
            Thread.currentThread().interrupt();
        }
        watchdog.shutdownNow();
    }

    void scheduleJob(Job job) {
        Schedule s = job.schedule();
        if (s.once()) {
            ticker.execute(() -> trigger(job));
        } else {
            ticker.scheduleAtFixedRate(() -> trigger(job), s.period().toMillis(), s.period().toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    /** ticker 线程上调用：只做判断和投递，几微秒就返回 */
    void trigger(Job job) {
        if (!job.getState().isAvailable() || job.isPaused()) return;
        if (!job.running.compareAndSet(false, true)) {         // 上一次还没跑完：跳过，而不是排队
            job.skipped.incrementAndGet();
            fire(new JobEvent(JobEvent.Type.SKIPPED_OVERLAP, job.getName(), 0, 0, null));
            return;
        }
        try {
            workers.execute(() -> execute(job));
        } catch (RejectedExecutionException e) {               // 线程池和队列都满了
            job.running.set(false);
            job.skipped.incrementAndGet();
            fire(new JobEvent(JobEvent.Type.REJECTED, job.getName(), 0, 0, e));
        }
    }

    private void execute(Job job) {
        var ex = new Execution(job);
        fire(new JobEvent(JobEvent.Type.STARTED, job.getName(), 1, 0, null));
        try {
            pipeline.invoke(ex);
            job.runs.incrementAndGet();
            fire(new JobEvent(JobEvent.Type.SUCCEEDED, job.getName(), ex.attempt(), ex.elapsedMillis(), null));
        } catch (Exception e) {
            job.runs.incrementAndGet();
            job.failures.incrementAndGet();
            fire(new JobEvent(JobEvent.Type.FAILED, job.getName(), ex.attempt(), ex.elapsedMillis(), e));
        } finally {
            job.lastDuration.set(ex.elapsedMillis());
            job.running.set(false);
        }
    }

    /** 监听器出错不影响调度（它们只是旁路通知，第 30 章标准 5） */
    void fire(JobEvent e) {
        for (var l : listeners) {
            try {
                l.accept(e);
            } catch (RuntimeException ex) {
                System.out.println("  [scheduler] 监听器出错已忽略：" + ex);
            }
        }
    }

    private static java.util.concurrent.ThreadFactory named(String prefix) {
        var n = new AtomicInteger();
        return r -> {
            var t = new Thread(r, prefix + "-" + n.incrementAndGet());   // 线程要有名字（第 32 章）
            t.setDaemon(true);
            return t;
        };
    }
}
