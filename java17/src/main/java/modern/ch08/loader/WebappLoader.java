package modern.ch08.loader;

import modern.ch05.core.Loader;
import modern.ch06.core.Lifecycle;
import modern.ch06.core.LifecycleException;
import modern.ch06.core.LifecycleListener;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Web 应用载入器（原书 org.apache.catalina.loader.WebappLoader）。
 * start() 时创建 WebappClassLoader；如果 reloadable，再启动一个后台任务，
 * 每隔 checkInterval 秒检查一次类文件是否被修改，修改了就通知 Context 重载。
 *
 * 原书：实现 Runnable，自己 new Thread，循环里 Thread.sleep。
 * 这里：ScheduledExecutorService.scheduleWithFixedDelay，停止时 shutdownNow 即可。
 */
public class WebappLoader implements Loader, Lifecycle {

    private final Path docBase;
    private final Runnable reloadAction;
    private boolean reloadable;
    private boolean delegate;
    private int checkInterval = 15;

    private volatile WebappClassLoader classLoader;
    private ScheduledExecutorService checker;

    /** reloadAction：检测到修改时要做的事——通常是 context::reload */
    public WebappLoader(Path docBase, Runnable reloadAction) {
        this.docBase = docBase;
        this.reloadAction = reloadAction;
    }

    public void setReloadable(boolean reloadable) { this.reloadable = reloadable; }
    public void setDelegate(boolean delegate) { this.delegate = delegate; }
    public void setCheckInterval(int seconds) { this.checkInterval = seconds; }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /** 第 12 章：由 Context 的 backgroundProcess() 调用，代替载入器自己的检查线程 */
    public boolean modified() {
        return classLoader != null && classLoader.modified();
    }

    public List<String> findRepositories() {
        return classLoader.findRepositories();
    }

    @Override
    public synchronized void start() {
        classLoader = new WebappClassLoader(docBase, new SharedClassLoader(), delegate);
        if (reloadable) {
            checker = Executors.newSingleThreadScheduledExecutor(r -> {
                var t = new Thread(r, "WebappLoader[" + docBase.getFileName() + "]");
                t.setDaemon(true);
                return t;
            });
            checker.scheduleWithFixedDelay(this::check, checkInterval, checkInterval, TimeUnit.SECONDS);
        }
    }

    private void check() {
        if (classLoader.modified()) {
            // 为什么不直接调用 reloadAction.run()？因为重载会 stop() 本载入器，
            // 而 stop() 要关掉当前这个检查线程——自己等自己结束会出问题。所以另开一个线程去重载，
            // 原书的 notifyContext() 也正是这么做的。
            new Thread(reloadAction, "WebappContextNotifier").start();
        }
    }

    @Override
    public synchronized void stop() throws LifecycleException {
        if (checker != null) {
            checker.shutdownNow();
            checker = null;
        }
        try {
            classLoader.close();          // Java 7+：释放打开的 JAR 文件句柄
        } catch (IOException e) {
            throw new LifecycleException("关闭类加载器失败: " + e);
        }
    }

    @Override public void addLifecycleListener(LifecycleListener listener) { }
    @Override public List<LifecycleListener> findLifecycleListeners() { return List.of(); }
    @Override public void removeLifecycleListener(LifecycleListener listener) { }
}
