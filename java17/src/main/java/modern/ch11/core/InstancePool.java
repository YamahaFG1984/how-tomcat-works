package modern.ch11.core;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * 原书 StandardWrapper 为 SingleThreadModel Servlet 维护的实例池的 Java 17 写法。
 *
 * SingleThreadModel 在 Servlet 2.4 被标记废弃、在 Servlet 6.0（Tomcat 10.1）被彻底删除，
 * 但「最多 N 个实例，每个实例同一时刻只给一个线程用，用完归还，没有空闲就等待」
 * 这个池化思想依然常用（数据库连接池、对象池）。
 *
 * 原书：Stack + synchronized + wait/notify + 三个计数器（nInstances、countAllocated、maxInstances）
 * 这里：Semaphore 控制同时借出的数量 + 并发双端队列存放空闲实例
 */
public final class InstancePool<T> {

    private final Supplier<T> factory;
    private final Semaphore permits;
    private final Deque<T> idle = new ConcurrentLinkedDeque<>();
    private final AtomicInteger created = new AtomicInteger();

    public InstancePool(Supplier<T> factory, int maxInstances) {
        this.factory = factory;
        this.permits = new Semaphore(maxInstances, true);      // 公平：先等的先拿到
    }

    /** 借一个实例；已借出 maxInstances 个时阻塞等待 */
    public T allocate() throws InterruptedException {
        permits.acquire();
        T instance = idle.pollFirst();
        if (instance == null) {
            instance = factory.get();                           // 池里没有空闲的，但还没到上限：新建
            created.incrementAndGet();
        }
        return instance;
    }

    /** 归还实例，唤醒一个等待者 */
    public void deallocate(T instance) {
        idle.offerFirst(instance);
        permits.release();
    }

    public int created() {
        return created.get();
    }
}
