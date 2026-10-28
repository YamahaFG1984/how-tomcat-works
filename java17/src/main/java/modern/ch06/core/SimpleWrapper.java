package modern.ch06.core;

import jakarta.servlet.Servlet;

import java.util.List;

import static modern.ch06.core.LifecycleEvent.Type.AFTER_START;
import static modern.ch06.core.LifecycleEvent.Type.AFTER_STOP;
import static modern.ch06.core.LifecycleEvent.Type.BEFORE_START;
import static modern.ch06.core.LifecycleEvent.Type.BEFORE_STOP;
import static modern.ch06.core.LifecycleEvent.Type.START;
import static modern.ch06.core.LifecycleEvent.Type.STOP;

/** 实现了 Lifecycle 的 Wrapper：停止时调用 Servlet 的 destroy()，这是 Servlet 生命周期的最后一步 */
public class SimpleWrapper extends modern.ch05.core.SimpleWrapper implements Lifecycle {

    private final LifecycleSupport lifecycle = new LifecycleSupport(this);
    private boolean started;

    @Override public void addLifecycleListener(LifecycleListener l) { lifecycle.addLifecycleListener(l); }
    @Override public List<LifecycleListener> findLifecycleListeners() { return lifecycle.findLifecycleListeners(); }
    @Override public void removeLifecycleListener(LifecycleListener l) { lifecycle.removeLifecycleListener(l); }

    @Override
    public synchronized void start() throws LifecycleException {
        System.out.println("Starting Wrapper " + name);
        if (started) {
            throw new LifecycleException("Wrapper already started");
        }
        lifecycle.fireLifecycleEvent(BEFORE_START, null);
        started = true;
        Lifecycle.start(loader);
        Lifecycle.start(pipeline.getBasic());
        lifecycle.fireLifecycleEvent(START, null);
        lifecycle.fireLifecycleEvent(AFTER_START, null);
    }

    @Override
    public synchronized void stop() throws LifecycleException {
        System.out.println("Stopping wrapper " + name);
        Servlet servlet = instance;
        if (servlet != null) {
            try {
                servlet.destroy();
            } catch (RuntimeException e) {
                e.printStackTrace();       // 原书 catch (Throwable t) {} 把异常吞掉了
            }
            instance = null;
        }
        if (!started) {
            throw new LifecycleException("Wrapper " + name + " not started");
        }
        lifecycle.fireLifecycleEvent(BEFORE_STOP, null);
        lifecycle.fireLifecycleEvent(STOP, null);
        started = false;
        Lifecycle.stop(pipeline.getBasic());
        Lifecycle.stop(loader);
        lifecycle.fireLifecycleEvent(AFTER_STOP, null);
    }
}
