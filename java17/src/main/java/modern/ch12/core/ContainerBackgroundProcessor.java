package modern.ch12.core;

import modern.ch05.core.Container;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 整棵容器树共用的后台线程（原书 Tomcat 5 ContainerBase.ContainerBackgroundProcessor）。
 * 每隔 delay 秒从顶层容器开始，递归调用每个容器的 backgroundProcess()。
 * 调用前把线程的上下文类加载器切换成该容器的 Web 应用类加载器，调用后恢复。
 */
public final class ContainerBackgroundProcessor {

    private ContainerBackgroundProcessor() {
    }

    public static ScheduledExecutorService start(Container top, int delaySeconds) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
            var t = new Thread(r, "ContainerBackgroundProcessor[" + top.getClass().getSimpleName() + "]");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleWithFixedDelay(() -> processChildren(top), delaySeconds, delaySeconds, TimeUnit.SECONDS);
        return executor;
    }

    static void processChildren(Container container) {
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        try {
            if (container.getLoader() != null && container.getLoader().getClassLoader() != null) {
                thread.setContextClassLoader(container.getLoader().getClassLoader());
            }
            if (container instanceof BackgroundProcessable bp) {
                bp.backgroundProcess();
            }
        } catch (RuntimeException e) {
            System.err.println("Exception invoking periodic operation: " + e);
        } finally {
            thread.setContextClassLoader(original);
        }
        for (Container child : container.findChildren()) {
            processChildren(child);
        }
    }
}
