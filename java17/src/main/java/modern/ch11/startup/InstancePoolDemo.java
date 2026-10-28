package modern.ch11.startup;

import modern.ch11.core.InstancePool;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 模拟 6 个请求同时访问一个「单线程模型」Servlet，实例池上限为 2：
 * 任意时刻最多 2 个请求在执行，总共只创建 2 个实例。
 * 运行：./run.sh ch11.startup.InstancePoolDemo
 */
public final class InstancePoolDemo {

    /** 假装是一个 Servlet：记录自己同时被几个线程使用 */
    static final class FakeServlet {
        private static final AtomicInteger IDS = new AtomicInteger();
        private final int id = IDS.incrementAndGet();
        private final AtomicInteger inUse = new AtomicInteger();

        void service(int request) throws InterruptedException {
            int concurrent = inUse.incrementAndGet();
            System.out.printf("请求 %d 由实例 #%d 处理（这个实例上同时有 %d 个线程）%n", request, id, concurrent);
            Thread.sleep(300);
            inUse.decrementAndGet();
        }
    }

    public static void main(String[] args) throws Exception {
        var pool = new InstancePool<>(FakeServlet::new, 2);
        var executor = Executors.newFixedThreadPool(6);    // 注意：ExecutorService 到 Java 19 才实现 AutoCloseable
        for (int i = 1; i <= 6; i++) {
            int request = i;
            executor.submit(() -> {
                FakeServlet servlet = pool.allocate();
                try {
                    servlet.service(request);
                } finally {
                    pool.deallocate(servlet);
                }
                return null;
            });
        }
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        System.out.println("一共创建了 " + pool.created() + " 个实例");
    }
}
