package modern.design.ch32;

import modern.design.kit.CompositeComponent;
import modern.design.kit.LifecycleBase;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 四个非功能性设计的小实验，每个都对应 Tomcat 里的一个决定：
 *   ① 线程封闭：请求对象只属于一个线程；共享的 Servlet 字段会丢更新（第 11 章）
 *   ② 写时复制：通知过程中监听器移除自己（第 6 章 LifecycleSupport）
 *   ③ 舱壁：慢任务和快任务共用一个线程池 vs 各用各的（第 14 章多个 Service / 第 28 章）
 *   ④ 一个后台线程驱动整棵组件树 vs 每个组件一个线程（第 12 章 backgroundProcess）
 *
 * 运行：./run.sh design.ch32.NonFunctionalDemo
 */
public final class NonFunctionalDemo {

    public static void main(String[] args) throws Exception {
        confinement();
        copyOnWrite();
        bulkhead();
        backgroundProcessor();
    }

    /* ① 线程封闭 */
    static final class CounterServlet {
        int hits;                                     // 共享的实例字段：所有请求线程都在改它

        void service(int[] perRequest) {
            hits++;                                   // 读-改-写，不是原子的
            perRequest[0]++;                          // 请求自己的对象：只有当前线程能看到
        }
    }

    static void confinement() throws Exception {
        System.out.println("① 线程封闭：16 个线程 × 每个 50000 次请求");
        var servlet = new CounterServlet();
        var pool = Executors.newFixedThreadPool(16);
        var perThreadTotals = new AtomicInteger();
        List<Future<?>> fs = new ArrayList<>();
        for (int t = 0; t < 16; t++) {
            fs.add(pool.submit(() -> {
                int[] request = new int[1];           // 相当于每个请求的 Request 对象
                for (int i = 0; i < 50_000; i++) servlet.service(request);
                perThreadTotals.addAndGet(request[0]);
            }));
        }
        for (var f : fs) f.get();
        pool.shutdown();
        System.out.printf("  请求对象里的计数合计 %d（正确），共享字段 hits = %d（丢了 %d 次更新）%n",
                perThreadTotals.get(), servlet.hits, perThreadTotals.get() - servlet.hits);
    }

    /* ② 写时复制 */
    static void copyOnWrite() {
        System.out.println("\n② 通知过程中，有一个监听器把自己移除（一次性监听器很常见）");
        for (List<Consumer<String>> listeners : List.<List<Consumer<String>>>of(new ArrayList<>(), new CopyOnWriteArrayList<>())) {
            var calls = new AtomicInteger();
            Consumer<String> once = new Consumer<>() {
                @Override public void accept(String e) {
                    calls.incrementAndGet();
                    listeners.remove(this);
                }
            };
            listeners.add(once);
            listeners.add(e -> calls.incrementAndGet());
            listeners.add(e -> calls.incrementAndGet());
            String kind = listeners.getClass().getSimpleName();
            try {
                for (var l : listeners) l.accept("START");
                System.out.printf("  %-20s 通知了 %d 个监听器，剩余 %d 个%n", kind, calls.get(), listeners.size());
            } catch (ConcurrentModificationException e) {
                System.out.printf("  %-20s ConcurrentModificationException（只通知到 %d 个）%n", kind, calls.get());
            }
        }
    }

    /* ③ 舱壁 */
    static void bulkhead() throws Exception {
        System.out.println("\n③ 舱壁：20 个慢报表（各 300ms）先提交，随后 5 个快查询（各 5ms）");
        ExecutorService shared = Executors.newFixedThreadPool(4);
        System.out.printf("  共用一个 4 线程池：快查询平均等待 %d ms%n", runMix(shared, shared));
        shared.shutdown();

        ExecutorService reports = Executors.newFixedThreadPool(2);
        ExecutorService queries = Executors.newFixedThreadPool(2);
        System.out.printf("  报表 2 线程 + 查询 2 线程：快查询平均等待 %d ms%n", runMix(reports, queries));
        reports.shutdown();
        queries.shutdown();
    }

    private static long runMix(ExecutorService reportPool, ExecutorService queryPool) throws Exception {
        for (int i = 0; i < 20; i++) reportPool.submit(() -> sleep(300));
        List<Future<Long>> fast = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            long submitted = System.nanoTime();
            fast.add(queryPool.submit(() -> {
                long waited = (System.nanoTime() - submitted) / 1_000_000;
                sleep(5);
                return waited;
            }));
        }
        long total = 0;
        for (var f : fast) total += f.get();
        return total / fast.size();
    }

    /* ④ 后台线程 */
    static final class Leaf extends LifecycleBase {
        final AtomicInteger ticks = new AtomicInteger();
        private Thread own;

        Leaf(String name) {
            super(name);
        }

        void backgroundProcess() {
            ticks.incrementAndGet();                  // 检查会话过期、类文件是否修改……
        }

        void startOwnThread() {                       // Tomcat 4 的做法：每个组件自己开一个线程
            own = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    backgroundProcess();
                    if (!sleep(50)) return;
                }
            }, getName() + "-bg");
            own.setDaemon(true);
            own.start();
        }

        void stopOwnThread() {
            if (own != null) own.interrupt();
        }

        @Override protected void startInternal() { }
        @Override protected void stopInternal() { }
    }

    static final class Tree extends CompositeComponent<Leaf> {
        Tree() {
            super("tree");
        }

        void backgroundProcess() {                    // Tomcat 5 起：父组件递归调用子组件
            children().forEach(Leaf::backgroundProcess);
        }
    }

    static void backgroundProcessor() throws Exception {
        System.out.println("\n④ 50 个组件的周期性任务，运行 1 秒");
        var tree = new Tree();
        for (int i = 0; i < 50; i++) tree.addChild(new Leaf("c" + i));
        tree.start();
        tree.children().forEach(Leaf::startOwnThread);
        sleep(200);
        System.out.printf("  每个组件一个线程：后台线程 %d 个%n", countThreads("-bg"));
        tree.children().forEach(Leaf::stopOwnThread);
        sleep(200);

        ScheduledExecutorService one = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "tree-background"));
        tree.children().forEach(l -> l.ticks.set(0));
        one.scheduleWithFixedDelay(tree::backgroundProcess, 0, 50, TimeUnit.MILLISECONDS);
        var latch = new CountDownLatch(1);
        one.schedule(latch::countDown, 1, TimeUnit.SECONDS);
        latch.await();
        System.out.printf("  一个后台线程递归驱动：后台线程 %d 个，每个组件被处理了约 %d 次%n",
                countThreads("-bg") + countThreads("tree-background"), tree.children().get(0).ticks.get());
        one.shutdown();
        tree.stop();
    }

    /** 线程命名本身就是可观测性设计：jstack 里一眼就能认出 http-nio-8080-exec-3 是谁 */
    private static long countThreads(String nameSuffix) {
        return Thread.getAllStackTraces().keySet().stream().filter(t -> t.getName().endsWith(nameSuffix)).count();
    }

    private static boolean sleep(long ms) {
        try {
            Thread.sleep(ms);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
