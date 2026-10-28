package modern.ch27;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

/**
 * 虚拟线程（Java 21，JEP 444）三个实验：
 *   ① 2000 个各睡 500ms 的任务：200 个平台线程的池 vs 每任务一个虚拟线程
 *   ② 虚拟线程长什么样：它被「挂载」在哪个载体线程上，睡醒后可能换一个
 *   ③ 钉住（pinning）：在 synchronized 里阻塞会占住载体线程（JDK 24 的 JEP 491 已修复）
 *
 * 运行（需要 JDK 21+）：./run.sh ch27.VirtualThreadsDemo
 */
public final class VirtualThreadsDemo {

    public static void main(String[] args) throws Exception {
        // 必须在第一个虚拟线程创建之前设置：载体线程池（ForkJoinPool）只用 2 个线程，让钉住的效果更明显
        System.setProperty("jdk.virtualThreadScheduler.parallelism", "2");
        System.out.println("JDK " + Runtime.version() + "，可用 CPU " + Runtime.getRuntime().availableProcessors());

        System.out.println("\n① 2000 个任务，每个阻塞 500ms");
        time("平台线程池（200 个线程）", Executors.newFixedThreadPool(200), 2000, VirtualThreadsDemo::sleepTask);
        time("虚拟线程（每任务一个）  ", Executors.newVirtualThreadPerTaskExecutor(), 2000, VirtualThreadsDemo::sleepTask);

        System.out.println("\n② 虚拟线程的样子");
        Thread vt = Thread.ofVirtual().name("demo-vt").start(() -> {
            System.out.println("  睡之前：" + Thread.currentThread());
            sleep(100);
            System.out.println("  睡之后：" + Thread.currentThread() + "（载体线程可能换了）");
        });
        vt.join();
        System.out.println("  isVirtual = " + vt.isVirtual() + "，isDaemon = " + vt.isDaemon());

        System.out.println("\n③ 钉住：8 个任务，载体线程只有 2 个，每个任务在锁里阻塞 500ms");
        time("synchronized 里 sleep   ", Executors.newVirtualThreadPerTaskExecutor(), 8, i -> {
            Object lock = new Object();                  // 每个任务一把锁，没有任何竞争
            synchronized (lock) {
                sleep(500);                              // JDK 21–23：虚拟线程被钉在载体线程上
            }
        });
        time("ReentrantLock 里 sleep  ", Executors.newVirtualThreadPerTaskExecutor(), 8, i -> {
            var lock = new ReentrantLock();
            lock.lock();
            try {
                sleep(500);                              // 可以卸载，载体线程去跑别的虚拟线程
            } finally {
                lock.unlock();
            }
        });
    }

    interface Task {
        void run(int i);
    }

    private static void sleepTask(int i) {
        sleep(500);
    }

    private static void time(String label, ExecutorService executor, int n, Task task) {
        long t0 = System.nanoTime();
        try (executor) {                                 // Java 19+：close() 等待所有任务完成
            IntStream.range(0, n).forEach(i -> executor.submit(() -> task.run(i)));
        }
        System.out.printf("  %s 用时 %.2f 秒%n", label, Duration.ofNanos(System.nanoTime() - t0).toMillis() / 1000.0);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
