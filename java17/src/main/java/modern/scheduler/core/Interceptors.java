package modern.scheduler.core;

import modern.design.kit.Pipeline;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** 常用的拦截器（相当于 Tomcat 自带的那些阀），都是普通的 Pipeline.Valve，可以随意组合 */
public final class Interceptors {

    private Interceptors() { }

    /** 记录每次执行的结果和耗时 */
    public static Pipeline.Valve<Execution> logging() {
        return (ex, next) -> {
            try {
                next.invoke(ex);
                ex.log("成功，用时 " + ex.elapsedMillis() + " ms");
            } catch (Exception e) {
                ex.log("失败：" + e);
                throw e;
            }
        };
    }

    /** 失败后重试，间隔指数增长（相当于把 next 调用多次） */
    public static Pipeline.Valve<Execution> retry(int maxAttempts, Duration firstBackoff, Scheduler scheduler) {
        return (ex, next) -> {
            long backoff = firstBackoff.toMillis();
            while (true) {
                try {
                    next.invoke(ex);
                    return;
                } catch (Exception e) {
                    // 次数用完、被中断、或者调度器正在停机（任务已不可用）：不再重试——停机时要尽快收尾
                    if (ex.attempt() >= maxAttempts || e instanceof InterruptedException
                            || !ex.job().getState().isAvailable()) throw e;
                    scheduler.fire(new JobEvent(JobEvent.Type.RETRYING, ex.jobName(), ex.attempt(), ex.elapsedMillis(), e));
                    Thread.sleep(backoff);
                    backoff *= 2;
                    ex.nextAttempt();
                }
            }
        };
    }

    /**
     * 超时：到点中断执行线程（任务要响应中断，和 Future.cancel(true) 的约定一样）。
     * 用一个共享的「看门狗」定时器，而不是每次执行再开一个线程（第 32 章：单线程拥有者）。
     */
    public static Pipeline.Valve<Execution> timeout(Duration limit, ScheduledExecutorService watchdog) {
        return (ex, next) -> {
            Thread worker = Thread.currentThread();
            Object lock = new Object();
            boolean[] done = {false};
            var alarm = watchdog.schedule(() -> {
                synchronized (lock) {
                    if (!done[0]) worker.interrupt();          // 加锁检查：任务结束后绝不会再中断这个线程
                }
            }, limit.toMillis(), TimeUnit.MILLISECONDS);
            try {
                next.invoke(ex);
            } catch (InterruptedException e) {
                throw new java.util.concurrent.TimeoutException("超过 " + limit.toMillis() + " ms 被中断");
            } finally {
                synchronized (lock) {
                    done[0] = true;
                }
                alarm.cancel(false);
                Thread.interrupted();                         // 清掉可能残留的中断标志，线程要还给线程池
            }
        };
    }
}
