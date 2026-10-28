package modern.ch06.core;

import modern.ch05.core.Container;
import modern.ch05.core.Valve;

import java.util.List;

import static modern.ch06.core.LifecycleEvent.Type.AFTER_START;
import static modern.ch06.core.LifecycleEvent.Type.AFTER_STOP;
import static modern.ch06.core.LifecycleEvent.Type.BEFORE_START;
import static modern.ch06.core.LifecycleEvent.Type.BEFORE_STOP;
import static modern.ch06.core.LifecycleEvent.Type.START;
import static modern.ch06.core.LifecycleEvent.Type.STOP;

/**
 * 实现了 Lifecycle 的 Context：启动时依次启动载入器、子容器、管道里的阀；停止时倒过来。
 * 于是只要启动/停止这一个顶层组件，整棵组件树都会跟着启动/停止。
 */
public class SimpleContext extends modern.ch05.core.SimpleContext implements Lifecycle {

    private final LifecycleSupport lifecycle = new LifecycleSupport(this);
    private boolean started;

    /** 第 19 章 Manager 的 list 命令用它显示 running / stopped */
    public boolean isRunning() {
        return started;
    }

    @Override public void addLifecycleListener(LifecycleListener l) { lifecycle.addLifecycleListener(l); }
    @Override public List<LifecycleListener> findLifecycleListeners() { return lifecycle.findLifecycleListeners(); }
    @Override public void removeLifecycleListener(LifecycleListener l) { lifecycle.removeLifecycleListener(l); }

    @Override
    public synchronized void start() throws LifecycleException {
        if (started) {
            throw new LifecycleException("SimpleContext has already started");
        }
        lifecycle.fireLifecycleEvent(BEFORE_START, null);
        started = true;
        Lifecycle.start(loader);                          // ① 下属组件
        for (Container child : findChildren()) {          // ② 子容器
            Lifecycle.start(child);
        }
        for (Valve valve : pipeline.getValves()) {        // ③ 管道里的阀（含基础阀）
            Lifecycle.start(valve);
        }
        Lifecycle.start(pipeline.getBasic());
        lifecycle.fireLifecycleEvent(START, null);
        lifecycle.fireLifecycleEvent(AFTER_START, null);
    }

    @Override
    public synchronized void stop() throws LifecycleException {
        if (!started) {
            throw new LifecycleException("SimpleContext has not been started");
        }
        lifecycle.fireLifecycleEvent(BEFORE_STOP, null);
        lifecycle.fireLifecycleEvent(STOP, null);
        started = false;
        Lifecycle.stop(pipeline.getBasic());              // 和启动顺序相反
        for (Valve valve : pipeline.getValves()) {
            Lifecycle.stop(valve);
        }
        for (Container child : findChildren()) {
            Lifecycle.stop(child);
        }
        Lifecycle.stop(loader);
        lifecycle.fireLifecycleEvent(AFTER_STOP, null);
    }
}
